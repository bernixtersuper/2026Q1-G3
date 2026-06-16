module "ml_orchestrator_lambda" {
  source = "./modules/python-lambda"

  function_name         = "${local.name_prefix}-ml-orchestrator"
  handler               = "orchestrator_lambda.handler"
  source_dir            = "${local.ml_training_root}/lambda_dist/orchestrator"
  iam_role_arn          = data.aws_iam_role.lab_role.arn
  timeout               = 60
  log_retention_in_days = var.log_retention_in_days

  vpc_subnet_ids         = module.vpc.private_subnets
  vpc_security_group_ids = [aws_security_group.db_client.id]

  environment_variables = {
    TRAINING_JOB_QUEUE_URL = aws_sqs_queue.ml-training.url
    DB_SECRET_ARN          = aws_db_instance.db.master_user_secret[0].secret_arn
    DB_URL                 = local.db_jdbc_url
  }
}

module "ml_worker_lambda" {
  source = "./modules/python-lambda"

  function_name         = "${local.name_prefix}-ml-worker"
  handler               = "worker_lambda.handler"
  source_dir            = "${local.ml_training_root}/lambda_dist/worker"
  iam_role_arn          = data.aws_iam_role.lab_role.arn
  timeout               = 300
  log_retention_in_days = var.log_retention_in_days

  environment_variables = {
    RECOMMENDATIONS_MODEL_S3_BUCKET = module.s3-private-buckets[var.ml_bucket_name].bucket_name
    ANALYTICS_SILVER_BUCKET         = module.s3_analytics_silver.bucket_name
    ML_FEATURES_PREFIX              = "ml_features"
  }
}

resource "aws_sqs_queue" "ml-training-dlq" {
  name = "ml-training-dlq"

  message_retention_seconds = 1209600 # 14 días — inspección manual de jobs fallidos
}

resource "aws_sqs_queue" "ml-training" {
  name = "ml-training-queue"

  visibility_timeout_seconds = 360
  message_retention_seconds  = 86400

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.ml-training-dlq.arn
    maxReceiveCount     = var.ml_training.sqs_max_receive_count
  })
}

# --- EventBridge cron → orquestador ---

resource "aws_cloudwatch_event_rule" "ml_training_schedule" {
  name                = "${local.name_prefix}-ml-training-schedule"
  description         = "Encola entrenamiento ML por tenant (Lambda orquestador)"
  schedule_expression = var.ml_training.schedule_expression
  state               = var.ml_training.schedule_enabled ? "ENABLED" : "DISABLED"
}

resource "aws_lambda_permission" "ml_orchestrator_eventbridge" {
  statement_id  = "AllowExecutionFromEventBridge"
  action        = "lambda:InvokeFunction"
  function_name = module.ml_orchestrator_lambda.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.ml_training_schedule.arn
}

resource "aws_cloudwatch_event_target" "ml_orchestrator" {
  rule      = aws_cloudwatch_event_rule.ml_training_schedule.name
  target_id = "orchestrator"
  arn       = module.ml_orchestrator_lambda.function_arn
}

# --- SQS → worker (batch con fallos parciales) ---

resource "aws_lambda_event_source_mapping" "ml_worker_sqs" {
  event_source_arn = aws_sqs_queue.ml-training.arn
  function_name    = module.ml_worker_lambda.function_arn
  batch_size       = var.ml_training.sqs_batch_size
  enabled          = true

  function_response_types = ["ReportBatchItemFailures"]
}
