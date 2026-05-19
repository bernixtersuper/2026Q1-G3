project_name = "menuqr"
aws_region   = "us-east-1"

vpc_cidr = "172.30.0.0/16"

images_bucket_name = "menuqr-images"
ml_bucket_name     = "menuqr-ml"
user_website_name  = "menuqr-user"
admin_website_name = "menuqr-admin"

db = {
  name     = "menuqr_db"
  username = "menuqr_admin"
}

backend = {
  image_tag     = "latest"
  desired_count = 2
}

ml_training = {
  schedule_expression = "cron(0 6 * * ? *)"
  schedule_enabled    = true
  sqs_batch_size      = 10
}
