# Eventos crudos (Parquet) desde Kinesis Firehose.
module "s3_analytics_events" {
  source = "./modules/s3-private"
  name   = "${local.name_prefix}-analytics-events"
}

# Eventos deduplicados / curados por Glue; los consume la Lambda ML worker.
module "s3_analytics_processed" {
  source = "./modules/s3-private"
  name   = "${local.name_prefix}-analytics-processed"
}
