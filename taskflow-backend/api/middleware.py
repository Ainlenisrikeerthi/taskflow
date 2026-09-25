import json
import logging
from django.http import JsonResponse
from django.utils.deprecation import MiddlewareMixin

logger = logging.getLogger(__name__)


class GlobalExceptionMiddleware(MiddlewareMixin):
    def process_exception(self, request, exception):
        # Allow DRF's custom_exception_handler to handle DRF API views
        if request.path.startswith('/api/'):
            logger.exception("Unhandled server exception on %s: %s", request.path, str(exception))
            return JsonResponse({
                "status": 500,
                "error": "Internal Server Error",
                "message": str(exception) or "An unexpected server error occurred",
                "path": request.path
            }, status=500)
        return None
