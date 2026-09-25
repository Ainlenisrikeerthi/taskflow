from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed
from rest_framework.permissions import BasePermission
from .models import User
from .security import decode_jwt


class JwtAuthentication(BaseAuthentication):
    def authenticate(self, request):
        auth_header = request.headers.get('Authorization')
        if not auth_header:
            return None

        parts = auth_header.split()
        if len(parts) != 2 or parts[0].lower() != 'bearer':
            return None

        token = parts[1].strip()
        try:
            email = decode_jwt(token)
            user = User.objects.filter(email=email).first()
            if not user:
                raise AuthenticationFailed("User not found for provided token")
            return (user, token)
        except AuthenticationFailed:
            raise
        except Exception as e:
            raise AuthenticationFailed(f"Authentication error: {str(e)}")


class IsAuthenticated(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and hasattr(request.user, 'id'))


class IsAdmin(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and hasattr(request.user, 'role') and request.user.role == 'ADMIN')


class IsUser(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and hasattr(request.user, 'role') and request.user.role == 'USER')
