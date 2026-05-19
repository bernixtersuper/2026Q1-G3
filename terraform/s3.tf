module "s3-private-buckets" {
  source = "./modules/s3-private"

  for_each = toset(local.private_bucket_names)

  name = each.value
}

module "s3-public-websites" {
  source = "./modules/s3-public-website"

  for_each = toset(local.public_bucket_names)

  name = each.value
}