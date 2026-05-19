resource "aws_security_group" "db_client" {
  name_prefix = "${local.name_prefix}-db-client-"
  vpc_id      = module.vpc.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "db" {
  name_prefix = "${local.name_prefix}-rds-"
  vpc_id      = module.vpc.vpc_id

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "proxy" {
  name_prefix = "${local.name_prefix}-rds-proxy-"
  vpc_id      = module.vpc.vpc_id

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "db_from_proxy" {
  security_group_id            = aws_security_group.db.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.proxy.id
}

resource "aws_vpc_security_group_ingress_rule" "proxy_from_clients" {
  security_group_id            = aws_security_group.proxy.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.db_client.id
}

resource "aws_vpc_security_group_egress_rule" "proxy_to_db" {
  security_group_id            = aws_security_group.proxy.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.db.id
}

resource "aws_vpc_security_group_egress_rule" "proxy_to_secretsmanager_vpce" {
  security_group_id            = aws_security_group.proxy.id
  description                  = "Credenciales RDS en Secrets Manager (VPC endpoint)"
  from_port                    = 443
  to_port                      = 443
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.vpc_interface_endpoints.id
}


resource "aws_db_instance" "db" {
  identifier = "${local.name_prefix}-postgres"

  db_name  = var.db.name
  username = var.db.username
  engine   = "postgres"

  engine_version        = "18.3"
  instance_class        = "db.t4g.micro"
  allocated_storage     = 20
  max_allocated_storage = 40
  storage_type          = "gp3"
  storage_encrypted     = true

  manage_master_user_password = true

  db_subnet_group_name   = module.vpc.database_subnet_group_name
  vpc_security_group_ids = [aws_security_group.db.id]

  multi_az            = true
  publicly_accessible = false
  skip_final_snapshot = true

  backup_retention_period    = 7
  auto_minor_version_upgrade = true

}


module "rds_proxy" {
  source  = "terraform-aws-modules/rds-proxy/aws"
  version = "4.4.0"

  name            = "${local.name_prefix}-rds-proxy"
  create_iam_role = false
  role_arn        = data.aws_iam_role.lab_role.arn
  require_tls     = true

  # Misma capa que RDS (database subnets); los clientes en private_subnets llegan por SG.
  vpc_subnet_ids         = module.vpc.database_subnets
  vpc_security_group_ids = [aws_security_group.proxy.id]

  engine_family = "POSTGRESQL"

  auth = {
    (var.db.username) = {
      description = "Credenciales maestras RDS (Secrets Manager)"
      secret_arn  = aws_db_instance.db.master_user_secret[0].secret_arn
    }
  }

  target_db_instance     = true
  db_instance_identifier = aws_db_instance.db.identifier
}

