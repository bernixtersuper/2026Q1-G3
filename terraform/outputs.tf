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

output "ml_training_queue_url" {
  value = aws_sqs_queue.ml-training.url
}

output "ml_training_dlq_url" {
  value = aws_sqs_queue.ml-training-dlq.url
}

output "alerts_sns_topic_arn" {
  value = aws_sns_topic.alerts.arn
}

output "cloudwatch_dashboard_name" {
  value = aws_cloudwatch_dashboard.operations.dashboard_name
}

output "cloudwatch_dashboard_url" {
  value = "https://${data.aws_region.current.region}.console.aws.amazon.com/cloudwatch/home?region=${data.aws_region.current.region}#dashboards/dashboard/${aws_cloudwatch_dashboard.operations.dashboard_name}"
}

output "ecs_backend_log_group" {
  value = aws_cloudwatch_log_group.ecs_backend.name
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

output "waf_web_acl_arn" {
  value = var.waf.enabled ? aws_wafv2_web_acl.alb[0].arn : null
}
