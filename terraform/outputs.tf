output "db_proxy_endpoint" {
  value = module.rds_proxy.proxy_endpoint
}

output "db_secret_arn" {
  value = aws_db_instance.db.master_user_secret[0].secret_arn
}

output "backend_api_url" {
  value = "http://${aws_lb.backend.dns_name}"
}

output "frontend_admin_s3_bucket" {
  value = module.s3-public-websites[var.admin_website_name].bucket_name
}

output "frontend_menu_s3_bucket" {
  value = module.s3-public-websites[var.user_website_name].bucket_name
}

output "frontend_admin_website_url" {
  value = module.s3-public-websites[var.admin_website_name].bucket_website_url
}

output "frontend_menu_website_url" {
  value = module.s3-public-websites[var.user_website_name].bucket_website_url
}

output "backend_ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "backend_ecs_cluster_name" {
  value = aws_ecs_cluster.backend.name
}

output "backend_ecs_service_name" {
  value = aws_ecs_service.backend.name
}

output "backend_images_s3_bucket" {
  value = local.images_bucket_name
}

output "backend_ml_s3_bucket" {
  value = local.ml_bucket_name
}

output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.main.id
}

output "cognito_user_pool_client_id" {
  value = aws_cognito_user_pool_client.admin_spa.id
}

output "cognito_issuer_url" {
  value = "https://cognito-idp.${data.aws_region.current.region}.amazonaws.com/${aws_cognito_user_pool.main.id}"
}
