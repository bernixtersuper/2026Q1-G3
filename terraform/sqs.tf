resource "aws_sqs_queue" "analytics_processor_dlq" {
  name                      = "${local.name_prefix}-analytics-processor-dlq"
  message_retention_seconds = 1209600 # 14 días
}
