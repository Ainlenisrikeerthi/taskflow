import time
from rest_framework.views import exception_handler
from rest_framework.response import Response
from rest_framework import status
from rest_framework.exceptions import APIException


class ResourceNotFoundException(APIException):
    status_code = status.HTTP_404_NOT_FOUND
    default_detail = "Resource not found"
    default_code = "not_found"


class BadRequestException(APIException):
    status_code = status.HTTP_400_BAD_REQUEST
    default_detail = "Bad request"
    default_code = "bad_request"


class DuplicateResourceException(APIException):
    status_code = status.HTTP_409_CONFLICT
    default_detail = "Resource already exists"
    default_code = "conflict"


class UnauthorizedAccessException(APIException):
    status_code = status.HTTP_403_FORBIDDEN
    default_detail = "You are not authorized to perform this action"
    default_code = "forbidden"


def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)

    request = context.get('request')
    path = request.path if request else ""

    status_code = response.status_code if response else status.HTTP_500_INTERNAL_SERVER_ERROR
    
    status_phrases = {
        400: "Bad Request",
        401: "Unauthorized",
        403: "Forbidden",
        404: "Not Found",
        409: "Conflict",
        500: "Internal Server Error"
    }
    error_phrase = status_phrases.get(status_code, "Error")

    message = ""
    errors = None

    if response is not None:
        if isinstance(response.data, dict):
            if 'detail' in response.data:
                message = str(response.data['detail'])
            elif 'message' in response.data:
                message = str(response.data['message'])
            else:
                errors = {}
                for k, v in response.data.items():
                    if isinstance(v, list):
                        errors[k] = "; ".join(str(item) for item in v)
                    else:
                        errors[k] = str(v)
                message = "Validation failed for request"
        elif isinstance(response.data, list):
            message = "; ".join(str(item) for item in response.data)
        else:
            message = str(response.data)
    else:
        message = str(exc) if str(exc) else "An unexpected server error occurred"

    error_data = {
        "status": status_code,
        "error": error_phrase,
        "message": message,
        "path": path,
    }
    if errors:
        error_data["errors"] = errors

    return Response(error_data, status=status_code)
