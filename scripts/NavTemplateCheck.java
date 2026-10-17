import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.servlet.support.csrf.CsrfRequestDataValueProcessor;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

public class NavTemplateCheck {
    static void require(boolean condition,String message) {
        if(!condition)throw new IllegalStateException(message);
    }
    public static void main(String[] args)throws Exception {
        var servlet=new MockServletContext();
        var application=new StaticWebApplicationContext();
        application.setServletContext(servlet);
        application.getBeanFactory().registerSingleton("webSecurityExpressionHandler",new DefaultWebSecurityExpressionHandler());
        application.getBeanFactory().registerSingleton("requestDataValueProcessor",new CsrfRequestDataValueProcessor());
        application.refresh();
        servlet.setAttribute("org.springframework.web.context.WebApplicationContext.ROOT",application);
        var resolver=new FileTemplateResolver();
        resolver.setPrefix(args[0]+"/templates/");resolver.setSuffix(".html");resolver.setTemplateMode(TemplateMode.HTML);resolver.setCharacterEncoding("UTF-8");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);engine.addDialect(new SpringSecurityDialect());
        for(String role:List.of("anonymous","USER","ADMIN")) {
            var request=new MockHttpServletRequest(servlet,"GET","/");
            request.setServletPath("/");request.getSession();
            request.setAttribute("_csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","nav-check-token"));
            var authentication=role.equals("anonymous")
                ?new AnonymousAuthenticationToken("check","anonymousUser",List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))
                :new UsernamePasswordAuthenticationToken("nav-check",null,List.of(new SimpleGrantedAuthority("ROLE_"+role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setUserPrincipal(authentication);
            var response=new MockHttpServletResponse();
            var exchange=JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response);
            var context=new WebContext(exchange,Locale.ENGLISH);
            context.setVariable("currentPage","home");
            context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,new ThymeleafEvaluationContext(application,null));
            context.setVariable("springRequestContext",new SpringWebMvcThymeleafRequestContext(new RequestContext(request,response,servlet,null),request));
            String html=engine.process(new TemplateSpec("layout",Set.of("navbar"),TemplateMode.HTML,null),context);
            boolean authenticated=!role.equals("anonymous");
            require(html.contains("class=\"nav-logout\"")==authenticated,role+": logout visibility");
            require(html.contains("nav-admin")==role.equals("ADMIN"),role+": admin visibility");
            require(html.contains("href=\"/account\" class=\"nav-login\"")==authenticated,role+": account visibility");
            require(html.contains("href=\"/login\" class=\"nav-login\"")==!authenticated,role+": login visibility");
            if(authenticated) {
                require(html.contains("action=\"/logout\""),role+": logout action");
                require(html.contains("name=\"_csrf\" value=\"nav-check-token\""),role+": logout CSRF token");
            }
            Files.writeString(Path.of(args[0],"nav-"+role+".html"),html);
            System.out.println(role+": navigation and POST logout token passed");
        }
        var request=new MockHttpServletRequest(servlet,"GET","/admin/dashboard");request.setServletPath("/admin/dashboard");request.getSession();
        request.setAttribute("_csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","nav-check-token"));
        var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),Locale.ENGLISH);
        context.setVariable("activePage","dashboard");
        context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,new ThymeleafEvaluationContext(application,null));
        context.setVariable("springRequestContext",new SpringWebMvcThymeleafRequestContext(new RequestContext(request,new MockHttpServletResponse(),servlet,null),request));
        String sidebar=engine.process(new TemplateSpec("admin/fragments",Set.of("sidebar"),TemplateMode.HTML,null),context);
        require(sidebar.contains("action=\"/logout\"")&&sidebar.contains("name=\"_csrf\" value=\"nav-check-token\""),"Admin sidebar logout token");
        System.out.println("Admin sidebar: POST logout and CSRF token passed");
        SecurityContextHolder.clearContext();application.close();
    }
}
