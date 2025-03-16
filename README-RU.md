## LiteRadar Android транспондер, MIT (c) 2022-2025 @miktim [EN](./README.md)

### Назначение  
Обмен в группе или передача на сервер собственного местоположения по протоколу UDP.  

Сборка .apk здесь: [./app/release/](./app/release/)  

### Требования  
Android 6+, HarmonyOS 3+  

### Использование  

#### Идентификация  
Индентификатором [транспондера](https://ru.ruwiki.ru/wiki/%D0%90%D0%97%D0%9D-%D0%92) при передаче данных по сети служит открытый ключ RSA512. Подтверждение подлинности обеспечивает цифровая подпись пакета данных. При ином способе обмена  идентификатор - base64 encoded SHA1 hash бинарного представления открытого ключа.  
Необязательное имя (псевдоним) транспондера имеет вспомогательное значение.

#### Режимы работы
<img src="./markdown/settings.png" 
     alt="Settings" height=400 width=240/>  
\- только трекер (по умолчанию). Данные о собственном местоположении в сеть не передаются.  
\- член UDP multicast группы: 224.0.9.090:9099. Передача и прием данных геолокации.  
\- UDP клиент с указанием IP адреса и порта или имени доступного хоста и порта сервера. Только передача данных.  

В последних двух случаях возможен выбор сетевого интерфейса.

#### Уведомления
<img
  src="./markdown/notification.png"
  alt="Notification" height=400 width=240/>  
Состояние транспондера указано в Android уведомлении. Сообщения об ошибках геолокации или сети сопровождаются звуковым сигналом.  
Для вызова настроек или восстановления фокуса приложения коснитесь текста уведомления.  

#### Взаимодействие с трекером
Транспондер передает данные трекеру в формате JSON.  
Intent Action: "org.literadar.tracker.ACTION"  
Intent extra data: "json"  
События и ответы трекера:  
Intent Action: "org.literadar.tracker.EVENT"  
Intent extra data: "json"  

Структура JSON пакета и управление трекером см. README https://github.com/miktim/mini-tracker  

#### Дополнительно
Настройки трекера хранятся в файле settings.json приложения.  
В случае аварийного завершения создается файл fatal.log.

#### Структура UDP пакета  
Данные пакуются в BigEndian порядке. Значения Double преобразуются в IEEE 754 long.


| Байт | Содержание |
|:----:|------------|
| 4    | magic number "LRdr" |
| 1    | unsigned byte, length of public key in bytes (n) |
| n    | public key |
| 2    | packet version |
| 1    | unsigned byte, length of transponder name (m) |
| m    | UTF-8 transponder name (0:16 chars) |
| 8    | long, timestamp in milliseconds (Epoch from 1 january 1970) |
| 2    | short, location timeout in seconds (> 0) |
| 8    | double, WGS-84 latititude in degrees (-90 : 90) |
| 8    | double, WGS-84 longitude in degrees (-180 : 180) |
| 2    | short, accuracy in meters (> 0)|
| ...  | user data |
| k    | signature |
| 1    | unsigned byte, signature length (k) |
