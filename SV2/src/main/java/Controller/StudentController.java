package Controller;

import Models.Student;
import Service.StudentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Scanner;

@RestController
@RequestMapping("/students")
public class StudentController {

    RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private StudentService studentService;

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        List<Student> students = studentService.getAllStudents();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        Student student = studentService.getStudentById(id);
        if (student != null) {
            return ResponseEntity.ok(student);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
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
                String response = restTemplate.postForObject("http://localhost:8080/login", requestEntity, String.class);
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

    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        Student existingStudent = studentService.getStudentById(id);
        if (existingStudent != null) {
            student.setId(id);
            Student updatedStudent = studentService.saveStudent(student);
            return ResponseEntity.ok(updatedStudent);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        Student existingStudent = studentService.getStudentById(id);
        if (existingStudent != null) {
            studentService.deleteStudent(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
