# CORS Handling in Spring Security

## 1. What is CORS and Preflight?
- Trình duyệt bảo vệ người dùng bằng cách chặn request gửi sang domain khác (vd: từ `localhost:3000` gọi API `localhost:8080`).
- Trình duyệt sẽ gửi một request `OPTIONS` (Preflight) để hỏi backend có cho phép không, TRƯỚC KHI gửi request thực tế (`POST`, `PUT`, `GET` với custom headers như `Authorization`).

## 2. Red Flag: Lỗi Mở CORS Tùy Tiện
- **KHÔNG ĐƯỢC DÙNG**: `allowedOrigins("*")` đi kèm với `allowCredentials(true)`. Đây là lỗi bảo mật nghiêm trọng và Spring sẽ quăng Exception sập server.
- **Cách đúng**: Khai báo rõ ràng danh sách origin được phép (VD: `http://localhost:5500`).

## 3. Cấu hình chuẩn trong Spring Security 6
- Trong `SecurityFilterChain`: Thêm `http.cors(Customizer.withDefaults());`
- Cấu hình Bean `CorsConfigurationSource`:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // Thay '*' bằng list cụ thể nếu có cookie/credentials
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:5500", "http://127.0.0.1:5500"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## 4. Troubleshooting: Bị chặn Preflight bởi JWT Filter
- **Triệu chứng**: Cấu hình CORS như trên rồi, nhưng DevTools vẫn báo lỗi CORS.
- **Diagnosis**: Request `OPTIONS` bị Spring Security Filter Chain ném vào `JwtAuthenticationFilter`, và dĩ nhiên request OPTIONS không có token, nên bị báo 401 Unauthorized và trả về, làm Browser hiểu nhầm là CORS fail.
- **Fix**: Trong code của `JwtAuthenticationFilter`, nếu `request.getMethod().equals("OPTIONS")`, hãy `filterChain.doFilter(request, response)` để thả cửa cho nó đi qua.
