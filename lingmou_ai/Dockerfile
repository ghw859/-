# ICBC · 灵枢 AI 服务（Day 10）
# 构建：docker build -t lingmou-ai .
# 运行：docker run -p 8000:8000 -e REDIS_URL=redis://redis:6379/0 -e DASHSCOPE_API_KEY=sk-xxx lingmou-ai
FROM python:3.10-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# 先装依赖，利用 Docker 层缓存（requirements 不变时重建秒级）
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    -i https://pypi.tuna.tsinghua.edu.cn/simple

COPY . .

EXPOSE 8000

# 容器内健康检查（slim 无 curl，用标准库 urllib）
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD python -c "import urllib.request;urllib.request.urlopen('http://127.0.0.1:8000/api/ai/health',timeout=4)" || exit 1

# 必须监听 0.0.0.0，否则宿主端口映射不通
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
