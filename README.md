# DosyaHub | Güvenli Dosya Depolama ve Paylaşım Platformu

DosyaHub, kullanıcıların dosyalarını güvenli şekilde depolamasını, yönetmesini ve paylaşmasını sağlayan modern bir web uygulamasıdır. Proje, PDF, JPG ve PNG formatlı dosyaları desteklemektedir.

![DosyaHub Logo](frontend/public/logo192.png)

## İçindekiler

- [Özellikler](#özellikler)
- [Sistem Gereksinimleri](#sistem-gereksinimleri)
- [Proje Mimarisi](#proje-mimarisi)
- [Kurulum](#kurulum)
  - [Backend](#backend-kurulumu)
  - [Frontend](#frontend-kurulumu)
  - [MinIO](#minio-kurulumu)
  - [PostgreSQL](#postgresql-kurulumu)
- [Çalıştırma](#çalıştırma)
- [API Endpointleri](#api-endpointleri)
- [Katkıda Bulunma](#katkıda-bulunma)
- [Lisans](#lisans)

## Özellikler

- 📁 PDF, PNG ve JPG formatında dosya yükleme, indirme ve silme
- 🔒 Güvenli kullanıcı kimlik doğrulama ve yetkilendirme
- 📑 Kullanıcı bazlı dosya yönetimi
- 🔍 Dosya arama ve filtreleme
- 📱 Responsive tasarım ile her cihazda kullanılabilirlik
- 🏆 Yüksek performanslı dosya işlemleri

## Sistem Gereksinimleri

- Java 11 veya üzeri
- Node.js 14 veya üzeri
- PostgreSQL 13 veya üzeri
- MinIO Server
- Maven 3.6 veya üzeri
- npm 6 veya üzeri
- Docker ve Docker Compose (isteğe bağlı)

## Proje Mimarisi

DosyaHub, aşağıdaki bileşenlerden oluşan modern bir mimariye sahiptir:

- **Backend**: Java ve Spring Boot kullanılarak RESTful API
- **Frontend**: React ve Material UI kullanılarak kullanıcı arayüzü
- **Veritabanı**: PostgreSQL
- **Dosya Depolama**: MinIO Object Storage
- **Güvenlik**: JWT tabanlı kimlik doğrulama

## Kurulum

Projeyi yerel geliştirme ortamınızda çalıştırmak için aşağıdaki adımları izleyin.

### PostgreSQL Kurulumu

#### Manuel Kurulum

1. [PostgreSQL resmi sitesinden](https://www.postgresql.org/download/) işletim sisteminize uygun PostgreSQL'i indirin ve kurun.
2. Aşağıdaki bilgilerle yeni bir veritabanı oluşturun:
   - Veritabanı adı: `dosyahub`
   - Kullanıcı adı: `postgres`
   - Şifre: `12345`
   - Port: `5432`

#### Docker ile Kurulum

```bash
docker run -d \
  --name postgres-dosyahub \
  -e POSTGRES_PASSWORD=12345 \
  -e POSTGRES_DB=dosyahub \
  -p 5432:5432 \
  postgres:13
```

### MinIO Kurulumu

#### Manuel Kurulum

1. [MinIO resmi sitesinden](https://min.io/download) işletim sisteminize uygun MinIO'yu indirin.
2. MinIO'yu aşağıdaki komutla başlatın:

```bash
# Windows için
minio.exe server C:\minio --console-address :9001

# Linux/Mac için
minio server /minio --console-address :9001
```

Varsayılan kimlik bilgileri:
- Access Key: `minioadmin`
- Secret Key: `minioadmin`

#### Docker ile Kurulum

```bash
docker run -d \
  --name minio-dosyahub \
  -p 9000:9000 \
  -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  -v minio-data:/data \
  minio/minio server /data --console-address ":9001"
```

MinIO kurulduktan sonra:
1. `http://localhost:9001` adresine giderek MinIO konsoluna erişin
2. `dosyahub` adında yeni bir bucket oluşturun

### Backend Kurulumu

1. Backend klasörüne gidin:
```bash
cd backend
```

2. Maven ile proje bağımlılıklarını yükleyin:
```bash
mvn clean install
```

3. Uygulamayı başlatın:
```bash
mvn spring-boot:run
```

Backend sunucusu varsayılan olarak `http://localhost:8080/api` adresinde çalışacaktır.

### Frontend Kurulumu

1. Frontend klasörüne gidin:
```bash
cd frontend
```

2. npm ile bağımlılıkları yükleyin:
```bash
npm install
```

3. Geliştirme sunucusunu başlatın:
```bash
npm start
```

Frontend uygulaması varsayılan olarak `http://localhost:3000` adresinde çalışacaktır.

## Çalıştırma

Tüm bileşenler kurulduktan sonra uygulamayı kullanmaya başlayabilirsiniz:

1. MinIO'yu başlatın
2. PostgreSQL'i başlatın
3. Backend'i başlatın
4. Frontend'i başlatın
5. `http://localhost:3000` adresine giderek uygulamaya erişin

## API Endpointleri

Backend API'si şu endpointleri sunmaktadır:

### Kimlik Doğrulama

- `POST /api/auth/register` - Yeni kullanıcı kaydı
- `POST /api/auth/login` - Kullanıcı girişi

### Dosya İşlemleri

- `GET /api/files` - Kullanıcı dosyalarını listele
- `POST /api/files/upload` - Yeni dosya yükle
- `GET /api/files/download/{fileName}` - Dosya indir
- `POST /api/files/download` - Dosya indir (request body ile)
- `DELETE /api/files/delete/{fileName}` - Dosya sil
- `PUT /api/files/delete` - Dosya sil (request body ile)

## Konfigürasyon

### Backend Konfigürasyonu

Backend konfigürasyonu `backend/src/main/resources/application.yml` dosyasında yapılabilir:

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dosyahub
    username: postgres
    password: 12345

minio:
  endpoint: http://localhost:9000
  port: 9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: dosyahub
  enabled: true
```

### Frontend Konfigürasyonu

Frontend API bağlantı ayarları `frontend/src/services/api.ts` dosyasında yapılabilir.

## Sorun Giderme

### CORS Sorunları

Backend'deki `CustomCorsFilter` sınıfı, CORS ayarlarını yapılandırır. Farklı domainler arası erişim sorunları yaşıyorsanız, buradaki ayarları kontrol edin.

### Bağlantı Hataları

- **PostgreSQL**: Veritabanı bağlantı hatası alırsanız, PostgreSQL'in çalıştığından ve erişilebilir olduğundan emin olun.
- **MinIO**: MinIO bağlantı hatası alırsanız, MinIO sunucusunun çalıştığından ve erişilebilir olduğundan emin olun.
- **Backend**: Swagger belgeleri `http://localhost:8080/api/swagger-ui.html` adresinden erişilebilir.

## Lisans

Bu proje [MIT lisansı](LICENSE) altında lisanslanmıştır. 