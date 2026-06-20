from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group: str = "ai-intelligence-service"

    core_service_url: str = "http://localhost:8080"
    ai_service_token: str = ""

    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama3.1:8b"
    ollama_timeout_ms: int = 30000

    es_uris: str = "http://localhost:9200"

    log_level: str = "INFO"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
