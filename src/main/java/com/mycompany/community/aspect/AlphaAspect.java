package com.mycompany.community.aspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

//@Component
//@Aspect
public class AlphaAspect {

    @Pointcut("execution(* com.mycompany.community.service.*.*(..))")
    public void pointcut() {

    }

    // 被织入之前执行
    @Before("pointcut()")
    public void before() {
        System.out.println("before");
    }

    // 被织入之后执行
    @After("pointcut()")
    public void after() {
        System.out.println("after");
    }

    // 被织入返回时执行
    @AfterReturning("pointcut()")
    public void afterRetuning() {
        System.out.println("afterRetuning");
    }

    // 被织入报异常时执行
    @AfterThrowing("pointcut()")
    public void afterThrowing() {
        System.out.println("afterThrowing");
    }

    // 被织入之前和之后都执行
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("around before");
        //执行被切入的业务逻辑
        Object obj = joinPoint.proceed();
        System.out.println("around after");
        return obj;
    }

}

