resource "aws_kinesis_stream" "menuqr_events" {
  name             = "menuqr-events"
  shard_count      = 1
  retention_period = 24
}
