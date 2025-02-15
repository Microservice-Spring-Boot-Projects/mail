package ml.dev.mail.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = ConfigProperties.class)
@TestPropertySource("classpath:application.properties")
public class EmailServiceTest {

    /*@InjectMocks
    private EmailService emailService;

    @Autowired
    private ConfigProperties configProperties;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
        System.out.println("setup");
    }

    @AfterEach
    void tearDown() {
        System.out.println("tear down");
    }
    
    @Test
    void testSendMail() {
        MailDTO mailDTO = new MailDTO();
        emailService.sendMail(mailDTO, "4");
        System.out.println("testSendMail");
    }*/
}
