resource "aws_cloudwatch_log_group" "ecs_backend" {
  name              = "/ecs/${local.name_prefix}-backend"
  retention_in_days = var.log_retention_in_days
}

locals {
  cloudwatch_alarm_actions = [aws_sns_topic.alerts.arn]

  cloudwatch_dashboard_widgets = [
    {
      type   = "text"
      x      = 0
      y      = 0
      width  = 24
      height = 1
      properties = {
        markdown = "# MenuQR — Operaciones\nALB · ECS · SQS · Lambda"
      }
    },
    {
      type   = "metric"
      x      = 0
      y      = 1
      width  = 8
      height = 6
      properties = {
        metrics = [
          ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_lb.backend.arn_suffix, { stat = "Sum" }],
        ]
        view   = "timeSeries"
        region = data.aws_region.current.region
        title  = "ALB — Requests"
        period = 300
      }
    },
    {
      type   = "metric"
      x      = 8
      y      = 1
      width  = 8
      height = 6
      properties = {
        metrics = [
          ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.backend.arn_suffix, { stat = "p95" }],
        ]
        view   = "timeSeries"
        region = data.aws_region.current.region
        title  = "ALB — Latencia p95 (s)"
        period = 300
      }
    },
    {
      type   = "metric"
      x      = 16
      y      = 1
      width  = 8
      height = 6
      properties = {
        metrics = [
          ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", aws_lb.backend.arn_suffix, { stat = "Sum" }],
          ["...", "UnHealthyHostCount", ".", ".", { stat = "Maximum" }],
        ]
        view   = "timeSeries"
        region = data.aws_region.current.region
        title  = "ALB — 5xx y hosts unhealthy"
        period = 300
      }
    },
    {
      type   = "metric"
      x      = 0
      y      = 7
      width  = 12
      height = 6
      properties = {
        metrics = [
          ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.backend.name, "ServiceName", aws_ecs_service.backend.name, { stat = "Average" }],
          [".", "MemoryUtilization", ".", ".", ".", ".", { stat = "Average" }],
        ]
        view   = "timeSeries"
        region = data.aws_region.current.region
        title  = "ECS — CPU y memoria"
        period = 300
      }
    },
    {
      type   = "metric"
      x      = 12
      y      = 7
      width  = 12
      height = 6
      properties = {
        metrics = [
          ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.ml-training.name, { stat = "Maximum" }],
          [".", ".", ".", aws_sqs_queue.ml-training-dlq.name, { stat = "Maximum", label = "DLQ" }],
          [".", "ApproximateAgeOfOldestMessage", ".", aws_sqs_queue.ml-training.name, { stat = "Maximum", yAxis = "right" }],
        ]
        view   = "timeSeries"
        region = data.aws_region.current.region
        title  = "SQS — Cola ML y edad del mensaje más viejo"
        period = 300
      }
    },
    {
      type   = "metric"
      x      = 0
      y      = 13
      width  = 12
      height = 6
      properties = {
        metrics = [
          ["AWS/Lambda", "Invocations", "FunctionName", module.ml_orchestrator_lambda.function_name, { stat = "Sum" }],
          [".", "Errors", ".", ".", { stat = "Sum" }],
        ]
        view   = "timeSeries"
        region = data.aws_region.current.region
        title  = "Lambda — Orquestador ML"
        period = 300
      }
    },
    {
      type   = "metric"
      x      = 12
      y      = 13
      width  = 12
      height = 6
      properties = {
        metrics = [
          ["AWS/Lambda", "Invocations", "FunctionName", module.ml_worker_lambda.function_name, { stat = "Sum" }],
          [".", "Errors", ".", ".", { stat = "Sum" }],
        ]
        view   = "timeSeries"
        region = data.aws_region.current.region
        title  = "Lambda — Worker ML"
        period = 300
      }
    },
  ]
}

resource "aws_cloudwatch_dashboard" "operations" {
  dashboard_name = "${local.name_prefix}-operations"

  dashboard_body = jsonencode({
    widgets = local.cloudwatch_dashboard_widgets
  })
}

resource "aws_cloudwatch_metric_alarm" "alb_unhealthy_hosts" {
  alarm_name          = "${local.name_prefix}-alb-unhealthy-hosts"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "UnHealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0
  alarm_description   = "Targets ECS unhealthy en el ALB del backend."
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = aws_lb.backend.arn_suffix
    TargetGroup  = aws_lb_target_group.backend.arn_suffix
  }

  alarm_actions = local.cloudwatch_alarm_actions
  ok_actions    = local.cloudwatch_alarm_actions
}

resource "aws_cloudwatch_metric_alarm" "alb_target_5xx" {
  alarm_name          = "${local.name_prefix}-alb-target-5xx"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = 300
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "Errores 5xx desde targets ECS del backend."
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = aws_lb.backend.arn_suffix
  }

  alarm_actions = local.cloudwatch_alarm_actions
  ok_actions    = local.cloudwatch_alarm_actions
}

resource "aws_cloudwatch_metric_alarm" "ml_worker_errors" {
  alarm_name          = "${local.name_prefix}-ml-worker-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 300
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "Errores en Lambda worker del pipeline ML."
  treat_missing_data  = "notBreaching"

  dimensions = {
    FunctionName = module.ml_worker_lambda.function_name
  }

  alarm_actions = local.cloudwatch_alarm_actions
  ok_actions    = local.cloudwatch_alarm_actions
}

resource "aws_cloudwatch_metric_alarm" "ml_orchestrator_errors" {
  alarm_name          = "${local.name_prefix}-ml-orchestrator-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 300
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "Errores en Lambda orquestador del pipeline ML."
  treat_missing_data  = "notBreaching"

  dimensions = {
    FunctionName = module.ml_orchestrator_lambda.function_name
  }

  alarm_actions = local.cloudwatch_alarm_actions
  ok_actions    = local.cloudwatch_alarm_actions
}


resource "aws_cloudwatch_metric_alarm" "ml_training_dlq_messages" {
  alarm_name          = "${local.name_prefix}-ml-training-dlq-messages"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0
  alarm_description   = "Mensajes en DLQ del pipeline ML; indica fallo persistente del worker tras reintentos."
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.ml-training-dlq.name
  }

  alarm_actions = local.cloudwatch_alarm_actions
  ok_actions    = local.cloudwatch_alarm_actions
}
