# condition-service

Сервис по обработке запросов jsonb и получению ответов

Отлично! Для минимального README.md в Notepad++ подойдет следующая структура.



```markdown

\# condition-service



Helm chart для развертывания сервиса condition-service в Kubernetes.



\## Требования



\- Kubernetes 1.19+

\- Helm 3.x



\## Установка



```bash

helm install my-condition-service ./condition-service-chart

```



\## Конфигурация



Параметры настраиваются в файле `condition-service-chart/values.yaml`.



Основные параметры:

\- `replicaCount` — количество реплик

\- `image.repository` — образ контейнера

\- `service.port` — порт сервиса



Подробное описание всех параметров смотрите в `values.yaml`.



\## Удаление



```bash

helm uninstall my-condition-service

```

```



