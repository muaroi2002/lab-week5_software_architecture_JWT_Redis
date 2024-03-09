package com.an.lap5.Controller;

import com.an.lap5.Models.Student;
import com.an.lap5.Repositories.StudentRepository;
import com.an.lap5.Service.StudentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

@RestController
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;
    private StudentService studentService;
    private Jedis jedis = new Jedis();
    private RestTemplate restTemplate;

    public StudentController() {
        this.restTemplate = new RestTemplate();
    }
    @GetMapping("/students")
    public List<Student> getAllStudents() {
        List<Student> list = studentRepository.findAll();
        return list;
    }
    @PostMapping("/students")
    public ResponseEntity<Student> createStudent(@RequestBody Student student, HttpSession session) {
        if (session.getAttribute("token") == null) {
            // Nếu không có token, yêu cầu người dùng đăng nhập
            System.out.println("Token not found. Please login first.");
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter username: ");
            String username = scanner.nextLine();

            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestBody = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            try {
                // Gửi yêu cầu đăng nhập và nhận token
                String response = restTemplate.postForObject("http://localhost:8081/login", requestEntity, String.class);
                // Lưu token vào session
                session.setAttribute("token", response);
                // Tiếp tục thực hiện tạo sinh viên
                return createStudentAfterLogin(student);
            } catch (HttpClientErrorException.BadRequest ex) {
                System.out.println("Login failed: Bad Request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Trả về lỗi xác thực
            }
        }

        // Lấy token từ session
        String token = (String) session.getAttribute("token");

        // Tạo một HttpHeaders object và thêm token vào header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        return createStudentAfterLogin(student);
    }

    // Phương thức để tạo sinh viên sau khi đã xác thực thành công
    private ResponseEntity<Student> createStudentAfterLogin(Student student) {
        Student createdStudent = studentService.saveStudent(student);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStudent);
    }
    @GetMapping("/{id}")
    public Student getStudentById(@PathVariable(value = "id") Long id, HttpSession session) {
        if (session.getAttribute("token") == null) {
            // Nếu không có token, in ra lỗi và kết thúc hàm
            System.out.println("Token not found. Please login first.");
            // Sử dụng Scanner để nhập username và password từ người dùng
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter username: ");
            String username = scanner.nextLine();

            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestBody = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            try {
                String response = restTemplate.postForObject("http://localhost:8080/login", requestEntity, String.class);
                // Lưu token vào session
                session.setAttribute("token", response);
                // Thực hiện tìm sinh viên sau khi xác thực thành công
                String key = String.valueOf(id);
                if (jedis.exists(key)) {
                    Student studentCash = new Student();
                    studentCash.setId(id);

                    // Lấy từ Redis dưới dạng hash
                    Map<String, String> studentData = jedis.hgetAll(key);

                    // Set các thuộc tính` `cho đối tượng Student từ giá trị lấy từ Redis
                    studentCash.setName(studentData.get("name"));
                    studentCash.setEmail(studentData.get("email"));

                    return studentCash;
                } else {
                    Student student = studentRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Student_id " + id + " not found"));

                    // Lưu từng trường và giá trị vào Redis dưới dạng hash
                    jedis.hset(key, "name", student.getName());
                    jedis.hset(key, "email", student.getEmail());

                    System.out.println("Saved in cache");
                    return student;
                }
            } catch (HttpClientErrorException.BadRequest ex) {
                System.out.println("Login failed: Bad Request");
                return null;
            }
        }

        // Nếu có token trong session, tiếp tục thêm sinh viên
        String token = (String) session.getAttribute("token");

        // Tạo một HttpHeaders object và thêm token vào header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        // Thêm sinh viên sau khi đã xác thực thành công
        String key = String.valueOf(id);
        if (jedis.exists(key)) {
            Student studentCash = new Student();
            studentCash.setId(id);

            // Lấy từ Redis dưới dạng hash
            Map<String, String> studentData = jedis.hgetAll(key);

            // Set các thuộc tính cho đối tượng Student từ giá trị lấy từ Redis
            studentCash.setName(studentData.get("name"));
            studentCash.setEmail(studentData.get("email"));

            return studentCash;
        } else {
            Student product = studentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Student_id " + id + " not found"));

            // Lưu từng trường và giá trị vào Redis dưới dạng hash
            jedis.hset(key, "name", product.getName());
            jedis.hset(key, "email", product.getEmail());

            System.out.println("Saved in cache");
            return product;
        }
    }
//@GetMapping("/students/{id}")
//public Student getStudentById(@PathVariable Long id) {
//    return studentRepository.findById(id).orElse(null);
//}



}
