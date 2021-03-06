$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");

	// 发送AJAX请求之前,将CSRF令牌设置到请求的消息头中.
   // var token = $("meta[name='_csrf']").attr("content");
   // var header = $("meta[name='_csrf_header']").attr("content");
   // // 在发送请求之前,对整个请求参数进行设置
   // $(document).ajaxSend(function(e, xhr, options){
   //     xhr.setRequestHeader(header, token);
   // });



	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	//发送异步请求（post）: 请求发送到哪个url; 规定连同请求发送到服务器的数据； 规定当请求成功时运行的函数。
	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title":title,"content":content},
		function(data) {
			//data为传回的数据
			data = $.parseJSON(data);
			// 在提示框中显示返回消息
			$("#hintBody").text(data.msg);
			// 显示提示框
			$("#hintModal").modal("show");
			// 2秒后,自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 如果添加成功（返回code=0），刷新页面
				if(data.code == 0) {
					window.location.reload();
				}
			}, 2000);
		}
	);

}