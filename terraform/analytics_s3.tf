module "s3_analytics" {
  source = "./modules/s3-private"
  name   = "${local.name_prefix}-analytics"
}
