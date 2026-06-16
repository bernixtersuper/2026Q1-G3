resource "aws_kinesis_firehose_delivery_stream" "menuqr_events" {
  name        = "${local.name_prefix}-events-firehose"
  destination = "extended_s3"

  kinesis_source_configuration {
    kinesis_stream_arn = aws_kinesis_stream.menuqr_events.arn
    role_arn           = local.lab_role_arn
  }

  extended_s3_configuration {
    role_arn            = local.lab_role_arn
    bucket_arn          = module.s3_analytics.bucket_arn
    prefix              = "events/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/"
    error_output_prefix = "errors/year=!{timestamp:yyyy}/month=!{timestamp:MM}/day=!{timestamp:dd}/!{firehose:error-output-type}/"

    buffering_size     = 64
    buffering_interval = 300

    compression_format = "UNCOMPRESSED"

    data_format_conversion_configuration {
      enabled = true

      input_format_configuration {
        deserializer {
          open_x_json_ser_de {
            case_insensitive                         = true
            convert_dots_in_json_keys_to_underscores = false
          }
        }
      }

      output_format_configuration {
        serializer {
          parquet_ser_de {
            compression = "SNAPPY"
          }
        }
      }

      schema_configuration {
        role_arn      = local.lab_role_arn
        database_name = aws_glue_catalog_database.menuqr.name
        table_name    = aws_glue_catalog_table.events_firehose.name
        region        = data.aws_region.current.region
      }
    }

    cloudwatch_logging_options {
      enabled         = true
      log_group_name  = aws_cloudwatch_log_group.firehose_events.name
      log_stream_name = "S3Delivery"
    }
  }
}

resource "aws_cloudwatch_log_group" "firehose_events" {
  name              = "/aws/kinesisfirehose/${local.name_prefix}-events"
  retention_in_days = var.log_retention_in_days
}

# Tabla Glue de referencia para conversión Parquet de Firehose
resource "aws_glue_catalog_table" "events_firehose" {
  name          = "events"
  database_name = aws_glue_catalog_database.menuqr.name

  table_type = "EXTERNAL_TABLE"

  parameters = {
    classification = "json"
  }

  storage_descriptor {
    location      = "s3://${module.s3_analytics.bucket_name}/events/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }

    columns {
      name = "eventId"
      type = "string"
    }
    columns {
      name = "eventType"
      type = "string"
    }
    columns {
      name = "tenantId"
      type = "string"
    }
    columns {
      name = "sessionId"
      type = "string"
    }
    columns {
      name = "timestamp"
      type = "string"
    }
    columns {
      name = "itemId"
      type = "string"
    }
    columns {
      name = "sectionId"
      type = "string"
    }
    columns {
      name = "metadata"
      type = "map<string,string>"
    }
  }
}
