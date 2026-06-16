module "analytics_processor_lambda" {
  source = "./modules/python-lambda"

  function_name         = "${local.name_prefix}-analytics-processor"
  handler               = "processor_lambda.handler"
  source_dir            = local.analytics_processor_dist
  iam_role_arn          = local.lab_role_arn
  timeout               = 60
  log_retention_in_days = var.log_retention_in_days

  environment_variables = {
    ANALYTICS_TABLE = aws_dynamodb_table.menuqr_analytics.name
    AWS_REGION      = data.aws_region.current.region
  }
}

resource "aws_lambda_event_source_mapping" "analytics_kinesis" {
  event_source_arn  = aws_kinesis_stream.menuqr_events.arn
  function_name     = module.analytics_processor_lambda.function_arn
  starting_position = "LATEST"
  batch_size        = 100
  enabled           = true

  bisect_batch_on_function_error = true
  maximum_retry_attempts         = 3
  function_response_types        = ["ReportBatchItemFailures"]

  depends_on = [module.analytics_processor_lambda]
}
