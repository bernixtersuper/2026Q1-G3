resource "aws_security_group" "fargate" {
  name_prefix = "${local.name_prefix}-fargate-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Quarkus desde ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_ecs_cluster" "backend" {
  name = "${local.name_prefix}-cluster"
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name_prefix}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = local.lab_role_arn
  task_role_arn            = local.lab_role_arn

  container_definitions = jsonencode([{
    name      = "backend"
    image     = local.backend_image
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs_backend.name
        "awslogs-region"        = data.aws_region.current.region
        "awslogs-stream-prefix" = "backend"
      }
    }

    environment = [
      { name = "DB_URL", value = local.db_jdbc_url },
      { name = "DB_SECRET_ARN", value = aws_db_instance.db.master_user_secret[0].secret_arn },
      { name = "DB_SECRET_CACHE_SECONDS", value = "300" },
      { name = "AWS_REGION", value = data.aws_region.current.region },
      { name = "S3_BUCKET", value = local.images_bucket_name },
      { name = "DYNAMO_ANALYTICS_TABLE", value = aws_dynamodb_table.menuqr_analytics.name },
      { name = "KINESIS_STREAM_NAME", value = aws_kinesis_stream.menuqr_events.name },
      { name = "ANALYTICS_KINESIS_ENABLED", value = "true" },
      { name = "ATHENA_WORKGROUP", value = aws_athena_workgroup.analytics.name },
      { name = "ATHENA_DATABASE", value = aws_glue_catalog_database.menuqr.name },
      { name = "ANALYTICS_PROCESSED_BUCKET", value = module.s3_analytics_processed.bucket_name },
      { name = "QUARKUS_PROFILE", value = "prod" },
      { name = "RECOMMENDATIONS_MODEL_S3_BUCKET", value = local.ml_bucket_name },
      { name = "COGNITO_ISSUER_URL", value = "https://cognito-idp.${data.aws_region.current.region}.amazonaws.com/${aws_cognito_user_pool.main.id}" },
      { name = "COGNITO_CLIENT_ID", value = aws_cognito_user_pool_client.admin_spa.id },
      { name = "QUARKUS_HTTP_CORS_ORIGINS", value = local.cors_allowed_origins },
      { name = "S3_PRESIGN_TTL_SECONDS", value = "3600" },
    ]
  }])

  depends_on = [aws_cloudwatch_log_group.ecs_backend]
}

resource "aws_ecs_service" "backend" {
  name            = "${local.name_prefix}-backend"
  cluster         = aws_ecs_cluster.backend.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.backend.desired_count
  launch_type     = "FARGATE"

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = module.vpc.private_subnets
    security_groups  = [aws_security_group.fargate.id, aws_security_group.db_client.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.backend_http]

  lifecycle {
    ignore_changes = [desired_count]
  }
}

resource "aws_appautoscaling_target" "backend" {
  max_capacity       = var.backend.autoscaling_max
  min_capacity       = var.backend.autoscaling_min
  resource_id        = "service/${aws_ecs_cluster.backend.name}/${aws_ecs_service.backend.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "backend_cpu" {
  name               = "${local.name_prefix}-backend-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.backend.resource_id
  scalable_dimension = aws_appautoscaling_target.backend.scalable_dimension
  service_namespace  = aws_appautoscaling_target.backend.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    target_value       = var.backend.autoscaling_target_cpu
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}
