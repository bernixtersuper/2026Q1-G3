resource "aws_s3_object" "glue_analytics_enrich_script" {
  bucket = module.s3_analytics.bucket_name
  key    = "glue-scripts/glue_analytics_enrich.py"
  source = "${path.module}/../glue-jobs/glue_analytics_enrich.py"
  etag   = filemd5("${path.module}/../glue-jobs/glue_analytics_enrich.py")
}

resource "aws_glue_job" "analytics_enrich" {
  name         = "${local.name_prefix}-analytics-enrich"
  role_arn     = local.lab_role_arn
  glue_version = "4.0"

  command {
    script_location = "s3://${module.s3_analytics.bucket_name}/${aws_s3_object.glue_analytics_enrich_script.key}"
    python_version  = "3"
  }

  number_of_workers = 2
  worker_type       = "G.1X"
  timeout           = 60

  default_arguments = {
    "--job-language"                     = "python"
    "--enable-metrics"                   = "true"
    "--enable-continuous-cloudwatch-log" = "true"
    "--ANALYTICS_TABLE"                  = aws_dynamodb_table.menuqr_analytics.name
    "--ANALYTICS_BUCKET"                 = module.s3_analytics.bucket_name
    "--GLUE_DATABASE"                    = aws_glue_catalog_database.menuqr.name
    "--EVENTS_TABLE"                     = aws_glue_catalog_table.events_firehose.name
    "--TOP_N"                            = "10"
    "--AWS_REGION"                       = data.aws_region.current.region
  }

  depends_on = [aws_s3_object.glue_analytics_enrich_script]
}

resource "aws_glue_trigger" "analytics_enrich_schedule" {
  name     = "${local.name_prefix}-analytics-enrich-daily"
  type     = "SCHEDULED"
  schedule = var.glue_analytics.enrich_schedule_expression
  enabled  = var.glue_analytics.enrich_schedule_enabled

  actions {
    job_name = aws_glue_job.analytics_enrich.name
  }
}

resource "aws_cloudwatch_metric_alarm" "glue_analytics_enrich_failed" {
  alarm_name          = "${local.name_prefix}-glue-analytics-enrich-failed"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "glue.driver.aggregate.numFailedTasks"
  namespace           = "Glue"
  period              = 300
  statistic           = "Maximum"
  threshold           = 0
  alarm_description   = "Glue analytics enrich job reportó tareas fallidas."
  treat_missing_data  = "notBreaching"

  dimensions = {
    JobName = aws_glue_job.analytics_enrich.name
  }

  alarm_actions = local.cloudwatch_alarm_actions
  ok_actions    = local.cloudwatch_alarm_actions
}
