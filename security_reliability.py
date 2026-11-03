#!/usr/bin/env python3
"""
Улучшение безопасности и надежности системы
"""

import asyncio
import logging
import time
import jwt
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any, Callable
from dataclasses import dataclass, asdict
from fastapi import HTTPException, Depends, status, Request
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel

logger = logging.getLogger(__name__)

# Security settings
JWT_SECRET = "your-secret-key-here"
JWT_ALGORITHM = "HS256"
JWT_EXPIRATION = 3600  # 1 час

security = HTTPBearer()

@dataclass
class User:
    """Модель пользователя"""
    id: str
    username: str
    email: str
    role: str  # admin, user, readonly
    created_at: datetime
    last_login: datetime
    is_active: bool = True

@dataclass
class SecurityPolicy:
    """Политика безопасности"""
    max_login_attempts: int = 5
    lockout_duration: int = 300  # 5 минут
    password_min_length: int = 8
    require_uppercase: bool = True
    require_lowercase: bool = True
    require_numbers: bool = True
    require_special: bool = True
    session_timeout: int = 3600  # 1 час
    max_concurrent_sessions: int = 3

@dataclass
class SecurityEvent:
    """Событие безопасности"""
    event_type: str
    user_id: str
    ip_address: str
    timestamp: datetime
    details: Dict[str, Any]
    severity: str  # info, warning, error, critical

class SecurityManager:
    """Менеджер безопасности"""
    
    def __init__(self, policy: SecurityPolicy):
        self.policy = policy
        self.users: Dict[str, User] = {}
        self.sessions: Dict[str, Dict] = {}
        self.failed_login_attempts: Dict[str, int] = {}
        self.security_events: List[SecurityEvent] = []
        self.user_lockouts: Dict[str, datetime] = {}
        
    def create_user(self, username: str, email: str, password: str, role: str = "user") -> User:
        """Создание пользователя"""
        user_id = f"user_{len(self.users) + 1}"
        
        # Валидация пароля
        if not self._validate_password(password):
            raise ValueError("Password does not meet security requirements")
            
        user = User(
            id=user_id,
            username=username,
            email=email,
            role=role,
            created_at=datetime.now(),
            last_login=datetime.now()
        )
        
        self.users[user_id] = user
        self._log_security_event("user_created", user_id, "127.0.0.1", {"username": username})
        
        return user
        
    def authenticate_user(self, username: str, password: str) -> Optional[User]:
        """Аутентификация пользователя"""
        # Проверка блокировки
        if self._is_user_locked(username):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Account temporarily locked due to multiple failed login attempts"
            )
            
        # Поиск пользователя
        user = None
        for u in self.users.values():
            if u.username == username and u.is_active:
                user = u
                break
                
        if not user:
            self._log_failed_attempt(username, "User not found")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid credentials"
            )
            
        # Проверка пароля (здесь должна быть проверка хешированного пароля)
        # if not self._verify_password(password, user.password_hash):
        if password != "correct_password":  # Временная проверка
            self._log_failed_attempt(username, "Invalid password")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid credentials"
            )
            
        # Сброс счетчика неудачных попыток
        self.failed_login_attempts.pop(username, None)
        
        # Обновление времени последнего входа
        user.last_login = datetime.now()
        
        self._log_security_event("user_login", user.id, "127.0.0.1", {"username": username})
        
        return user
        
    def create_jwt_token(self, user: User) -> str:
        """Создание JWT токена"""
        payload = {
            "user_id": user.id,
            "username": user.username,
            "role": user.role,
            "exp": datetime.utcnow() + timedelta(seconds=self.policy.session_timeout),
            "iat": datetime.utcnow()
        }
        
        token = jwt.encode(payload, JWT_SECRET, algorithm=JWT_ALGORITHM)
        
        # Сохранение сессии
        session_id = f"session_{user.id}_{int(time.time())}"
        self.sessions[session_id] = {
            "user_id": user.id,
            "created_at": datetime.now(),
            "last_activity": datetime.now(),
            "ip_address": "127.0.0.1"
        }
        
        return token
        
    def verify_jwt_token(self, token: str) -> Optional[User]:
        """Проверка JWT токена"""
        try:
            payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
            user_id = payload["user_id"]
            
            # Проверка существования пользователя
            if user_id not in self.users:
                return None
                
            user = self.users[user_id]
            
            # Проверка активности пользователя
            if not user.is_active:
                return None
                
            # Проверка срока действия токена
            exp = payload["exp"]
            if datetime.utcnow() > datetime.fromtimestamp(exp):
                return None
                
            # Обновление активности сессии
            session_id = None
            for sid, session in self.sessions.items():
                if session["user_id"] == user_id:
                    session_id = sid
                    session["last_activity"] = datetime.now()
                    break
                    
            return user
            
        except jwt.PyJWTError:
            return None
            
    def logout_user(self, user: User):
        """Выход пользователя из системы"""
        # Удаление сессий пользователя
        sessions_to_remove = []
        for session_id, session in self.sessions.items():
            if session["user_id"] == user.id:
                sessions_to_remove.append(session_id)
                
        for session_id in sessions_to_remove:
            del self.sessions[session_id]
            
        self._log_security_event("user_logout", user.id, "127.0.0.1", {"username": user.username})
        
    def check_permission(self, user: User, required_role: str) -> bool:
        """Проверка прав пользователя"""
        role_hierarchy = {
            "readonly": 1,
            "user": 2,
            "admin": 3
        }
        
        user_level = role_hierarchy.get(user.role, 0)
        required_level = role_hierarchy.get(required_role, 0)
        
        return user_level >= required_level
        
    def _validate_password(self, password: str) -> bool:
        """Валидация пароля"""
        if len(password) < self.policy.password_min_length:
            return False
            
        if self.policy.require_uppercase and not any(c.isupper() for c in password):
            return False
            
        if self.policy.require_lowercase and not any(c.islower() for c in password):
            return False
            
        if self.policy.require_numbers and not any(c.isdigit() for c in password):
            return False
            
        if self.policy.require_special and not any(c in "!@#$%^&*()_+-=[]{}|;:,.<>?" for c in password):
            return False
            
        return True
        
    def _log_failed_attempt(self, username: str, reason: str):
        """Логирование неудачной попытки входа"""
        attempts = self.failed_login_attempts.get(username, 0) + 1
        self.failed_login_attempts[username] = attempts
        
        if attempts >= self.policy.max_login_attempts:
            self.user_lockouts[username] = datetime.now()
            self._log_security_event("account_lockout", username, "127.0.0.1", {
                "attempts": attempts,
                "reason": reason
            })
        else:
            self._log_security_event("failed_login", username, "127.0.0.1", {
                "attempts": attempts,
                "reason": reason
            })
            
    def _is_user_locked(self, username: str) -> bool:
        """Проверка блокировки пользователя"""
        if username in self.user_lockouts:
            lock_time = self.user_lockouts[username]
            if datetime.now() - lock_time < timedelta(seconds=self.policy.lockout_duration):
                return True
            else:
                # Срок блокировки истек, удаляем запись
                del self.user_lockouts[username]
                
        return False
        
    def _log_security_event(self, event_type: str, user_id: str, ip_address: str, details: Dict[str, Any]):
        """Логирование события безопасности"""
        event = SecurityEvent(
            event_type=event_type,
            user_id=user_id,
            ip_address=ip_address,
            timestamp=datetime.now(),
            details=details,
            severity=self._get_event_severity(event_type)
        )
        
        self.security_events.append(event)
        
        # Сохранение в файл (здесь может быть интеграция с SIEM системой)
        self._save_security_event(event)
        
        logger.warning(f"Security event: {event_type} for user {user_id}")
        
    def _get_event_severity(self, event_type: str) -> str:
        """Определение уровня серьезности события"""
        critical_events = ["user_created", "account_lockout", "unauthorized_access"]
        warning_events = ["failed_login", "permission_denied"]
        
        if event_type in critical_events:
            return "critical"
        elif event_type in warning_events:
            return "warning"
        else:
            return "info"
            
    def _save_security_event(self, event: SecurityEvent):
        """Сохранение события безопасности"""
        # Здесь может быть интеграция с системой логирования или SIEM
        pass

class ReliabilityManager:
    """Менеджер надежности"""
    
    def __init__(self):
        self.health_checks: Dict[str, Dict] = {}
        self.circuit_breakers: Dict[str, 'CircuitBreaker'] = {}
        self.retry_policies: Dict[str, 'RetryPolicy'] = {}
        self.monitoring_metrics: Dict[str, List] = {}
        
    def register_health_check(self, name: str, check_func: Callable, interval: int = 30):
        """Регистрация проверки здоровья"""
        self.health_checks[name] = {
            "check_func": check_func,
            "interval": interval,
            "last_check": None,
            "status": "unknown",
            "error": None
        }
        
    async def run_health_checks(self):
        """Выполнение проверок здоровья"""
        for name, check in self.health_checks.items():
            try:
                result = await check["check_func"]()
                check["last_check"] = datetime.now()
                check["status"] = "healthy" if result else "unhealthy"
                check["error"] = None
                
                # Логирование
                if result:
                    logger.info(f"Health check {name}: OK")
                else:
                    logger.warning(f"Health check {name}: FAILED")
                    
            except Exception as e:
                check["last_check"] = datetime.now()
                check["status"] = "error"
                check["error"] = str(e)
                
                logger.error(f"Health check {name} error: {e}")
                
    def get_health_status(self) -> Dict[str, Dict]:
        """Получение статуса здоровья системы"""
        status = {}
        for name, check in self.health_checks.items():
            status[name] = {
                "status": check["status"],
                "last_check": check["last_check"].isoformat() if check["last_check"] else None,
                "error": check["error"]
            }
        return status
        
    def register_circuit_breaker(self, name: str, failure_threshold: int = 5, recovery_timeout: int = 60):
        """Регистрация выключателя цепи"""
        self.circuit_breakers[name] = CircuitBreaker(name, failure_threshold, recovery_timeout)
        
    def execute_with_circuit_breaker(self, name: str, func: Callable, *args, **kwargs):
        """Выполнение функции с выключателем цепи"""
        if name not in self.circuit_breakers:
            return func(*args, **kwargs)
            
        breaker = self.circuit_breakers[name]
        return breaker.execute(func, *args, **kwargs)
        
    def register_retry_policy(self, name: str, max_retries: int = 3, backoff_factor: float = 1.0):
        """Регистрация политики повторных попыток"""
        self.retry_policies[name] = RetryPolicy(name, max_retries, backoff_factor)
        
    async def execute_with_retry(self, name: str, func: Callable, *args, **kwargs):
        """Выполнение функции с повторными попытками"""
        if name not in self.retry_policies:
            return await func(*args, **kwargs)
            
        policy = self.retry_policies[name]
        return await policy.execute(func, *args, **kwargs)
        
    def record_metric(self, name: str, value: float, tags: Dict[str, str] = None):
        """Запись метрики"""
        if name not in self.monitoring_metrics:
            self.monitoring_metrics[name] = []
            
        self.monitoring_metrics[name].append({
            "value": value,
            "timestamp": datetime.now(),
            "tags": tags or {}
        })
        
    def get_metrics(self, name: str = None) -> Dict[str, List]:
        """Получение метрик"""
        if name:
            return self.monitoring_metrics.get(name, [])
        return self.monitoring_metrics

class CircuitBreaker:
    """Выключатель цепи"""
    
    def __init__(self, name: str, failure_threshold: int = 5, recovery_timeout: int = 60):
        self.name = name
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.failure_count = 0
        self.last_failure_time = None
        self.state = "closed"  # closed, open, half_open
        
    def execute(self, func: Callable, *args, **kwargs):
        """Выполнение функции с выключателем цепи"""
        if self.state == "open":
            if self._should_attempt_reset():
                self.state = "half_open"
            else:
                raise Exception(f"Circuit breaker {self.name} is open")
                
        try:
            result = func(*args, **kwargs)
            self._on_success()
            return result
        except Exception as e:
            self._on_failure()
            raise e
            
    def _should_attempt_reset(self) -> bool:
        """Проверка необходимости сброса"""
        if self.last_failure_time:
            return (datetime.now() - self.last_failure_time).total_seconds() >= self.recovery_timeout
        return False
        
    def _on_success(self):
        """Обработка успешного выполнения"""
        self.failure_count = 0
        self.state = "closed"
        
    def _on_failure(self):
        """Обработка неудачного выполнения"""
        self.failure_count += 1
        self.last_failure_time = datetime.now()
        
        if self.failure_count >= self.failure_threshold:
            self.state = "open"

class RetryPolicy:
    """Политика повторных попыток"""
    
    def __init__(self, name: str, max_retries: int = 3, backoff_factor: float = 1.0):
        self.name = name
        self.max_retries = max_retries
        self.backoff_factor = backoff_factor
        
    async def execute(self, func: Callable, *args, **kwargs):
        """Выполнение функции с повторными попытками"""
        last_exception = None
        
        for attempt in range(self.max_retries + 1):
            try:
                return await func(*args, **kwargs)
            except Exception as e:
                last_exception = e
                
                if attempt < self.max_retries:
                    wait_time = self.backoff_factor * (2 ** attempt)
                    logger.warning(f"Retry attempt {attempt + 1}/{self.max_retries} for {self.name}, waiting {wait_time}s")
                    await asyncio.sleep(wait_time)
                    
        raise last_exception

# Dependency functions для FastAPI
def get_current_user(security_credentials: HTTPAuthorizationCredentials = Depends(security)) -> User:
    """Зависимость для получения текущего пользователя"""
    # Здесь должна быть реализация проверки JWT токена
    # Временная реализация
    return User(
        id="temp_user",
        username="temp_user",
        email="temp@example.com",
        role="user",
        created_at=datetime.now(),
        last_login=datetime.now()
    )

def require_role(required_role: str):
    """Декоратор для проверки роли пользователя"""
    def decorator(func):
        async def wrapper(*args, **kwargs):
            user = kwargs.get("current_user") or args[0] if args else None
            if not user or not security_manager.check_permission(user, required_role):
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="Insufficient permissions"
                )
            return await func(*args, **kwargs)
        return wrapper
    return decorator

# Глобальные экземпляры
security_policy = SecurityPolicy()
security_manager = SecurityManager(security_policy)
reliability_manager = ReliabilityManager()