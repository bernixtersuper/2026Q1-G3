output "bucket_id" {
  value = aws_s3_bucket.this.id
}

output "bucket_arn" {
  value = aws_s3_bucket.this.arn
}

output "bucket_name" {
  value = aws_s3_bucket.this.bucket
}

output "bucket_website_url" {
  value = "http://${aws_s3_bucket_website_configuration.this.website_endpoint}"
}

output "bucket_website_endpoint" {
  value = aws_s3_bucket_website_configuration.this.website_endpoint
}
