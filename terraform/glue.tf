resource "aws_glue_catalog_database" "menuqr" {
  name = "${local.name_prefix}_analytics"
}

resource "aws_glue_crawler" "events" {
  name          = "${local.name_prefix}-events-crawler"
  role          = local.lab_role_arn
  database_name = aws_glue_catalog_database.menuqr.name

  s3_target {
    path = "s3://${module.s3_analytics_events.bucket_name}/"
  }

  schedule = var.glue_analytics.crawler_schedule

  schema_change_policy {
    delete_behavior = "LOG"
    update_behavior = "UPDATE_IN_DATABASE"
  }
}
