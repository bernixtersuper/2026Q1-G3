# Bronze: eventos crudos (Firehose → events/)
module "s3_analytics_bronze" {
  source = "./modules/s3-private"
  name   = "${local.name_prefix}-analytics-events"
}

# Eventos deduplicados / curados por Glue (opcional; reservado para export/ad-hoc).
module "s3_analytics_processed" {
  source = "./modules/s3-private"
  name   = "${local.name_prefix}-analytics-processed"
}

# Silver: features ML materializadas (Glue enrich → ml_features/)
module "s3_analytics_silver" {
  source = "./modules/s3-private"
  name   = "${local.name_prefix}-analytics-silver"
}

moved {
  from = module.s3_analytics
  to   = module.s3_analytics_bronze
}
