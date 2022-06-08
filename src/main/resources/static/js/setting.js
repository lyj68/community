$(function(){
    // 异步提交表单：点击提交按钮触发异步提交upload方法
    $("#uploadForm").submit(upload);
});

function upload() {
    $.ajax({
        url: "http://upload-z2.qiniup.com",
        method: "post",
        // 因为要上传的是文件形式：不要把表单内容转为字符串，所以要用false
        processData: false,
        // 不让jquery设置文件类型
        contentType: false,
        // 传递的是表单数据取json
        data: new FormData($("#uploadForm")[0]),
        // 成功的话如何处理：用一个函数表示
        success: function(data) {
            if(data && data.code == 0) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    // 前端的key来获取的文件：给我们的controller
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    return false;
}