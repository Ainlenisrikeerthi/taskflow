import datetime
import bcrypt
import jwt
from django.conf import settings
from rest_framework.exceptions import AuthenticationFailed
from .models import User


def check_password(raw_password: str, hashed_password: str) -> bool:
    if not raw_password or not hashed_password:
        return False
    try:
        raw_bytes = raw_password.encode('utf-8')
        hash_str = hashed_password.strip()
        # Some libraries produce $2a$ or $2b$ prefixes; both are valid bcrypt
        hash_bytes = hash_str.encode('utf-8')
        try:
            return bcrypt.checkpw(raw_bytes, hash_bytes)
        except ValueError:
            # Fallback if $2a$ vs $2b$ format nuance
            if hash_str.startswith('$2a$'):
                alt_hash = ('$2b$' + hash_str[4:]).encode('utf-8')
                return bcrypt.checkpw(raw_bytes, alt_hash)
            elif hash_str.startswith('$2b$'):
                alt_hash = ('$2a$' + hash_str[4:]).encode('utf-8')
                return bcrypt.checkpw(raw_bytes, alt_hash)
            return False
    except Exception:
        return False


def hash_password(raw_password: str) -> str:
    salt = bcrypt.gensalt(rounds=10)
    return bcrypt.hashpw(raw_password.encode('utf-8'), salt).decode('utf-8')


def generate_jwt(email: str) -> str:
    secret = settings.JWT_SECRET
    expiration_ms = settings.JWT_EXPIRATION_MS
    now = datetime.datetime.now(datetime.timezone.utc)
    exp = now + datetime.timedelta(milliseconds=expiration_ms)

    payload = {
        'sub': email.strip().lower(),
        'iat': int(now.timestamp()),
        'exp': int(exp.timestamp())
    }
    # JJWT with 512-bit key uses HS512
    token = jwt.encode(payload, secret.encode('utf-8'), algorithm='HS512')
    return token


def decode_jwt(token: str) -> str:
    secret = settings.JWT_SECRET
    try:
        payload = jwt.decode(
            token,
            secret.encode('utf-8'),
            algorithms=['HS512', 'HS256', 'HS384']
        )
        email = payload.get('sub')
        if not email:
            raise AuthenticationFailed("Invalid token payload: missing subject")
        return email.strip().lower()
    except jwt.ExpiredSignatureError:
        raise AuthenticationFailed("Authentication token has expired")
    except jwt.PyJWTError as e:
        raise AuthenticationFailed(f"Invalid authentication token: {str(e)}")
