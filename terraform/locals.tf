locals {
  azs = slice(data.aws_availability_zones.available.names, 0, 2)

  cidrs = cidrsubnets(var.vpc_cidr,
    8, 8,
    8, 8,
    8, 8
  )
  public_subnet_cidrs  = slice(local.cidrs, 0, 2)
  private_subnet_cidrs = slice(local.cidrs, 2, 4)
  db_subnet_cidrs      = slice(local.cidrs, 4, 6)

  name_prefix  = lower(replace(var.project_name, "_", "-"))
  vpc_name     = "${local.name_prefix}-vpc"
  lab_role_arn = data.aws_iam_role.lab_role.arn

  private_bucket_names = [var.images_bucket_name, var.ml_bucket_name]
  public_bucket_names  = [var.user_website_name, var.admin_website_name]

  images_bucket_name = module.s3-private-buckets[var.images_bucket_name].bucket_name
  ml_bucket_name     = module.s3-private-buckets[var.ml_bucket_name].bucket_name

  db_jdbc_url = "jdbc:postgresql://${module.rds_proxy.proxy_endpoint}:${aws_db_instance.db.port}/${var.db.name}"

  route_table_ids = module.vpc.private_route_table_ids
  gateway_endpoints = [
    "com.amazonaws.${data.aws_region.current.region}.s3",
    "com.amazonaws.${data.aws_region.current.region}.dynamodb",
  ]

  secretsmanager_endpoint_service = "com.amazonaws.${data.aws_region.current.region}.secretsmanager"
  sqs_endpoint_service            = "com.amazonaws.${data.aws_region.current.region}.sqs"
  ecr_api_endpoint_service        = "com.amazonaws.${data.aws_region.current.region}.ecr.api"
  ecr_dkr_endpoint_service        = "com.amazonaws.${data.aws_region.current.region}.ecr.dkr"

  ml_training_root = "${path.module}/../ml-training"

  backend_image = "${aws_ecr_repository.backend.repository_url}:${var.backend.image_tag}"
}
