output "state_bucket_name" {
  description = "S3 bucket for Terraform remote state"
  value       = aws_s3_bucket.terraform_state.id
}

output "lock_table_name" {
  description = "DynamoDB table for Terraform state locking"
  value       = aws_dynamodb_table.terraform_locks.name
}

output "state_key" {
  description = "Object key inside the state bucket"
  value       = "${local.name_prefix}/terraform.tfstate"
}
