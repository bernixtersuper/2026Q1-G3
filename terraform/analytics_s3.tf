# Event Storage: Firehose → events/ (Parquet crudo)
module "s3_analytics_events" {
  source = "./modules/s3-private"
  name   = "${local.name_prefix}-analytics-events"
}

# ML Analytics: Glue enrich → ml_features/ (features por tenant+día)
module "s3_analytics_ml" {
  source = "./modules/s3-private"
  name   = "${local.name_prefix}-analytics-silver"
}

moved {
  from = module.s3_analytics
  to   = module.s3_analytics_events
}

moved {
  from = module.s3_analytics_bronze
  to   = module.s3_analytics_events
}

moved {
  from = module.s3_analytics_silver
  to   = module.s3_analytics_ml
}
