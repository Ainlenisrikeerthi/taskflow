import os
import urllib.parse
from pathlib import Path
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent

# Load environment variables from .env
load_dotenv(BASE_DIR / '.env')

SECRET_KEY = os.getenv('DJANGO_SECRET_KEY', 'taskflow-django-super-secret-key-replacement-2026')

DEBUG = os.getenv('DEBUG', 'True').lower() in ('true', '1', 't')

ALLOWED_HOSTS = ['*']

INSTALLED_APPS = [
    'django.contrib.contenttypes',
    'django.contrib.staticfiles',
    'corsheaders',
    'rest_framework',
    'api',
]

MIDDLEWARE = [
    'corsheaders.middleware.CorsMiddleware',
    'django.middleware.security.SecurityMiddleware',
    'django.middleware.common.CommonMiddleware',
    'api.middleware.GlobalExceptionMiddleware',
]

ROOT_URLCONF = 'taskflow_project.urls'

WSGI_APPLICATION = 'taskflow_project.wsgi.application'

# -----------------------------------------------------------------------------
# Database Configuration (Supabase PostgreSQL)
# -----------------------------------------------------------------------------
raw_db_url = os.getenv('DB_URL', '')
db_user = os.getenv('DB_USERNAME', 'postgres')
db_password = os.getenv('DB_PASSWORD', '')

clean_url = raw_db_url.replace('jdbc:', '')
parsed_db = urllib.parse.urlparse(clean_url)

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': parsed_db.path.lstrip('/') or 'postgres',
        'USER': parsed_db.username or db_user,
        'PASSWORD': parsed_db.password or db_password,
        'HOST': parsed_db.hostname or 'localhost',
        'PORT': str(parsed_db.port or 5432),
    }
}

# -----------------------------------------------------------------------------
# CORS Configuration (Allows frontend React dev and production builds)
# -----------------------------------------------------------------------------
FRONTEND_URL = os.getenv('FRONTEND_URL', 'http://localhost:5173')

CORS_ALLOW_ALL_ORIGINS = True
CORS_ALLOW_CREDENTIALS = True
CORS_ALLOW_HEADERS = ['*']
CORS_ALLOW_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS']

# -----------------------------------------------------------------------------
# REST Framework Configuration
# -----------------------------------------------------------------------------
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'api.authentication.JwtAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.AllowAny',
    ],
    'EXCEPTION_HANDLER': 'api.exceptions.custom_exception_handler',
    'UNAUTHENTICATED_USER': None,
}

# -----------------------------------------------------------------------------
# JWT Settings (Compatible with Spring Boot JJWT HMAC-SHA)
# -----------------------------------------------------------------------------
JWT_SECRET = os.getenv('JWT_SECRET', 'd1ba7902964d7cb1f3875e7f6a04097ee4d2a54ec0f7b347d4d268108a2afa63b4250c84319b59669504763690919ad9c379b51ab100034f8e90fe973bf06088')
JWT_EXPIRATION_MS = int(os.getenv('JWT_EXPIRATION', '86400000'))

# -----------------------------------------------------------------------------
# Email Configuration (SMTP)
# -----------------------------------------------------------------------------
EMAIL_HOST = os.getenv('EMAIL_HOST', 'smtp.gmail.com')
EMAIL_PORT = int(os.getenv('EMAIL_PORT', '587'))
EMAIL_HOST_USER = os.getenv('EMAIL_USERNAME', '')
EMAIL_HOST_PASSWORD = os.getenv('EMAIL_PASSWORD', '')
EMAIL_USE_TLS = True
DEFAULT_FROM_EMAIL = os.getenv('EMAIL_USERNAME', 'noreply@taskflow.com')

# -----------------------------------------------------------------------------
# OpenRouter / AI Settings
# -----------------------------------------------------------------------------
OPENROUTER_API_KEY = os.getenv('OPENROUTER_API_KEY', '')
OPENROUTER_PRIMARY_MODEL = os.getenv('OPENROUTER_PRIMARY_MODEL', 'openrouter/free')
OPENROUTER_FALLBACK_MODEL = os.getenv('OPENROUTER_FALLBACK_MODEL', 'openrouter/free')
OPENROUTER_SITE_URL = os.getenv('OPENROUTER_SITE_URL', 'http://localhost:5173')
OPENROUTER_APP_NAME = os.getenv('OPENROUTER_APP_NAME', 'TaskFlow')

# -----------------------------------------------------------------------------
# Local Code Runner Settings
# -----------------------------------------------------------------------------
LOCAL_JAVA_COMMAND = os.getenv('LOCAL_JAVA_COMMAND', 'java')
LOCAL_JAVAC_COMMAND = os.getenv('LOCAL_JAVAC_COMMAND', 'javac')
LOCAL_PYTHON_COMMAND = os.getenv('LOCAL_PYTHON_COMMAND', 'python')
LOCAL_COMPILE_TIMEOUT_SECONDS = int(os.getenv('LOCAL_COMPILE_TIMEOUT_SECONDS', '10'))
LOCAL_RUN_TIMEOUT_SECONDS = int(os.getenv('LOCAL_RUN_TIMEOUT_SECONDS', '5'))
LOCAL_MAX_OUTPUT_CHARS = int(os.getenv('LOCAL_MAX_OUTPUT_CHARS', '20000'))

# -----------------------------------------------------------------------------
# Internationalization & Static
# -----------------------------------------------------------------------------
LANGUAGE_CODE = 'en-us'
TIME_ZONE = 'UTC'
USE_I18N = True
USE_TZ = True
STATIC_URL = 'static/'
DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'
