resource "aws_lambda_function" "validate_cpf_lambda" {
  function_name    = "validate-cpf-lambda"
  role            = "arn:aws:iam::385515961485:role/LabRole"
  handler         = "com.example.fast_food_lambda.src.lambda.CpfLambdaHandler::handleRequest"
  runtime         = "java21"
  memory_size     = 512
  timeout         = 15
  filename        = "lambda.zip"
  source_code_hash = filebase64sha256("lambda.zip")
}
