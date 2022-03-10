package com.mycompany.community.controller;


import com.mycompany.community.service.AlphaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Controller
// 给AlphaController类取个浏览器访问名字
@RequestMapping("/alpha")
public class AlphaController {

    //调用sevice层
    @Autowired
    private AlphaService alphaService;


    @RequestMapping("/hello")
    @ResponseBody
    // 给savhello方法取个访问名字，使它能够被访问
    public String savHello(){
        return "Hello!";
    }

    //要使浏览器访问该层，就需要加下面的注解
    @RequestMapping("/data")
    @ResponseBody
    public  String getData(){
        return alphaService.find();
    }

     //怎么获得请求数据和访问数据
    @RequestMapping("/http")
    //为什么是void？是因为课可以通过response直接获得数据
    // request为请求对象，response为响应对象
    public void http(HttpServletRequest request, HttpServletResponse response){
        //读取请求数据
        System.out.println(request.getMethod());  //获取请求方式
        System.out.println(request.getContextPath()); //获取请求路径
        //获取所有请求行的key;请求行是key,value结构
        //获得的是迭代器类型
        Enumeration<String> enumeration = request.getHeaderNames();
        while (enumeration.hasMoreElements()){
            String name = enumeration.nextElement();
            String value = request.getHeader(name);
            System.out.println(name+": "+value);
        }
        System.out.println(request.getParameter("code"));


        //reponse 是给浏览器返回的响应数据

        //设置返回类型
        //设置类型为网页文本类型
        response.setContentType("text/html;charset=utf-8");
        //获取输出流
        try (PrintWriter writer = response.getWriter();){

            //通过writer向浏览器打印网页
            writer.write("<h1>牛客网</h1>");

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    //GET请求
    //用于获取网页数据

    //获取学生数据第一页，限制20组
    // /students?current=1&limit=20
    //限制：路径和请求方法
    @RequestMapping(path = "/students", method = RequestMethod.GET)
    //返回请求字符串
    @ResponseBody
    //只要对应参数和你传的参数名一致
    //@RequestParam 对参数默认值进行详细设置
    public String getStudents(
            @RequestParam(name = "current", required = false,defaultValue = "1") int current,
            @RequestParam(name = "limit", required = false,defaultValue = "10")int limit){
        System.out.println(current);
        System.out.println(limit);
        return "some students";
    }

    // /students/123
    @RequestMapping(path = "/student/{id}",method = RequestMethod.GET)
    @ResponseBody
    // @PathVariable获取路径里面的参数
    public String getStudent(@PathVariable("id") int id){
        System.out.println(id);
        return "a student";
    }

    // POST请求
    //首先浏览器得打开一个网页
    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    //只要和表单名称类型一致就可以
    public String saveStudent(String name , int age){
        System.out.println(name);
        System.out.println(age);
        return "success";

    }

    //向浏览器返回响应数据
    //响应html数据
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public ModelAndView getTeacher(){
        //先实例化对象
        ModelAndView mav = new ModelAndView();
        //往里传参数:模板需要多少个对象，你就给多少
        mav.addObject("name","zhangsan");
        mav.addObject("age",30);
        //模板要放在templates里面,默认为html，其实是viem.html
        mav.setViewName("/demo/view");
        return mav;

    }

    @RequestMapping(path = "/school", method = RequestMethod.GET)
    //把model当参数传入
    public String getSchool(Model model){
        model.addAttribute("name","Beijing university");
        model.addAttribute("age",80);
        //return view的路径
        return "/demo/view";
    }

    //向浏览器响应json数据
    //通常一般是在异步请求
    // java对象 -> json字符串 -> js对象
    //要返回json必须要@ResponseBody
    @RequestMapping(path = "/emp", method = RequestMethod.GET)
    //看到这个注解和这个方法，会自动转成json
    @ResponseBody
    public Map<String,Object> getEmp(){
        HashMap<String, Object> emp = new HashMap<>();
        emp.put("name","张三");
        emp.put("age",29);
        emp.put("salary",80000);
        return emp;

    }
    @RequestMapping(path = "/emps", method = RequestMethod.GET)
    //返回集合
    @ResponseBody
    public List<Map<String,Object>> getEmps(){
        List<Map<String,Object>> list = new ArrayList<>();
        HashMap<String, Object> emp = new HashMap<>();
        emp.put("name","张三");
        emp.put("age",29);
        emp.put("salary",80000);
        list.add(emp);
        emp = new HashMap<>();
        emp.put("name","李四");
        emp.put("age",23);
        emp.put("salary",8000);
        list.add(emp);
        emp = new HashMap<>();
        emp.put("name","王五");
        emp.put("age",25);
        emp.put("salary",8900);
        list.add(emp);
        return list;

    }


}
