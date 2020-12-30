/*
 * FileName：WxApiCtrl.java 
 * <p>
 * Copyright (c) 2017-2020, <a href="http://www.webcsn.com">hermit (794890569@qq.com)</a>.
 * <p>
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/gpl-3.0.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.wxmp.wxapi.ctrl;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.alibaba.fastjson.JSON;
import com.qq.weixin.mp.aes.AesException;
import com.qq.weixin.mp.aes.WXBizMsgCrypt;
import com.wxmp.core.exception.WxErrorException;
import com.wxmp.wxapi.process.*;
import com.wxmp.wxapi.service.ComponentService;
import com.wxmp.wxapi.vo.*;
import com.wxmp.wxcms.domain.AccessTokens;
import com.wxmp.wxcms.mapper.AccessTokenDao;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSONObject;
import com.wxmp.core.common.BaseCtrl;
import com.wxmp.core.util.AjaxResult;
import com.wxmp.core.util.DateUtil;
import com.wxmp.core.util.UploadUtil;
import com.wxmp.core.util.wx.SignUtil;
import com.wxmp.wxapi.service.MyService;
import com.wxmp.wxcms.domain.AccountFans;
import com.wxmp.wxcms.domain.MsgNews;
import com.wxmp.wxcms.domain.MsgText;
import com.wxmp.wxcms.service.MsgNewsService;
import com.wxmp.wxcms.service.MsgTextService;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;


/**
 * 微信与开发者服务器交互接口
 */
@Controller
@RequestMapping("/wxapi")
public class WxApiCtrl extends BaseCtrl{
	
	private static Logger log = LogManager.getLogger(WxApiCtrl.class);
	
	@Resource
	private MyService myService;
	@Resource
	private MsgTextService msgTextService;
	@Resource
	private MsgNewsService msgNewsService;

	@Autowired
	ComponentService componentService;

	
	/**
	 * GET请求：进行URL、Tocken 认证；
	 * 1. 将token、timestamp、nonce三个参数进行字典序排序
	 * 2. 将三个参数字符串拼接成一个字符串进行sha1加密
	 * 3. 开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
	 */
	@RequestMapping(value = "/{account}/message",  method = RequestMethod.GET)
	public @ResponseBody String doGet(HttpServletRequest request,@PathVariable String account) {
		//如果是多账号，根据url中的account参数获取对应的MpAccount处理即可
		
		Set<String> keySet = request.getParameterMap().keySet();
		Iterator<String> iterator = keySet.iterator();
        while(iterator.hasNext()){  
            //如果存在，则调用next实现迭代  
            String key=iterator.next();    
            log.info("key: " + key + " value: " + request.getParameterMap().get(key));
        }
		
		
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount(account);//获取缓存中的唯一账号
		if(mpAccount != null){
			String token = mpAccount.getToken();//获取token，进行验证；
			String signature = request.getParameter("signature");// 微信加密签名
			String timestamp = request.getParameter("timestamp");// 时间戳
			String nonce = request.getParameter("nonce");// 随机数
			String echostr = request.getParameter("echostr");// 随机字符串
			
			// 校验成功返回  echostr，成功成为开发者；否则返回error，接入失败
			if (SignUtil.validSign(signature, token, timestamp, nonce)) {
				return echostr;
			}
		}
		return "error";
	}
	
	/**
	 * POST 请求：进行消息处理；
	 * */
	@RequestMapping(value = "/{account}/message", method = RequestMethod.POST)
	public @ResponseBody String doPost(HttpServletRequest request,@PathVariable String account,HttpServletResponse response) {
		//处理用户和微信公众账号交互消息
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount(account);
		try {
			MsgRequest msgRequest = MsgXmlUtil.parseXml(request);//获取发送的消息
			return myService.processMsg(msgRequest,mpAccount);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
			return "error";
		}
	}
	
	//创建微信公众账号菜单
	@RequestMapping(value = "/publishMenu")
	public ModelAndView publishMenu() throws WxErrorException  {
		JSONObject rstObj = null;
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();
		if(mpAccount != null){
			rstObj = myService.publishMenu(mpAccount);
			if(rstObj != null){//成功，更新菜单组
				if(rstObj.containsKey("menu_id")){
					ModelAndView mv = new ModelAndView("common/success");
					mv.addObject("successMsg", "创建菜单成功");
					return mv;
				}else if(rstObj.containsKey("errcode") && rstObj.getIntValue("errcode") == 0){
					ModelAndView mv = new ModelAndView("common/success");
					mv.addObject("successMsg", "创建菜单成功");
					return mv;
				}
			}
		}
		
		ModelAndView mv = new ModelAndView("common/failure");
		String failureMsg = "创建菜单失败，请检查菜单：可创建最多3个一级菜单，每个一级菜单下可创建最多5个二级菜单。";
		if(rstObj != null){
			failureMsg += ErrCode.errMsg(rstObj.getIntValue("errcode"));
		}
		mv.addObject("failureMsg", failureMsg);
		return mv;
	}
	
	//删除微信公众账号菜单
	@RequestMapping(value = "/deleteMenu")
	public ModelAndView deleteMenu(HttpServletRequest request) throws WxErrorException {
		JSONObject rstObj = null;
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		if(mpAccount != null){
			rstObj = myService.deleteMenu(mpAccount);
			if(rstObj != null && rstObj.getIntValue("errcode") == 0){
				ModelAndView mv = new ModelAndView("common/success");
				mv.addObject("successMsg", "删除菜单成功");
				return mv;
			}
		}
		ModelAndView mv = new ModelAndView("common/failure");
		String failureMsg = "删除菜单失败";
		if(rstObj != null){
			failureMsg += ErrCode.errMsg(rstObj.getIntValue("errcode"));
		}
		mv.addObject("failureMsg", failureMsg);
		return mv;
	}
	


	/**
	 * 同步用户标签列表
	 * @return
	 */
	@RequestMapping(value = "/syncUserTagList")
	@ResponseBody
	public AjaxResult syncUserTagList() throws WxErrorException {
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		if(mpAccount != null){
			boolean flag = myService.syncUserTagList(mpAccount);
			if(flag){
				return AjaxResult.success();
			}
		}
		return AjaxResult.failure();
	}

	//根据用户的ID更新用户信息
	@RequestMapping(value = "/syncAccountFans")
	@ResponseBody
	public AjaxResult syncAccountFans(String openId) throws WxErrorException {
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		if (mpAccount != null) {
			AccountFans fans = myService.syncAccountFans(openId, mpAccount, true);//同时更新数据库
			if (fans != null) {
				return AjaxResult.success(fans);
			}
		}
		return AjaxResult.failure();
	}
	
	//获取永久素材
	@RequestMapping(value = "/syncMaterials")
	public AjaxResult syncMaterials(MaterialArticle materialArticle) throws WxErrorException {
		List<MaterialArticle> materialList = new ArrayList<MaterialArticle>();
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		Material material = WxApiClient.syncBatchMaterial(MediaType.News, materialArticle.getPage(), materialArticle.getPageSize(),mpAccount);
		if(material != null){

			List<MaterialItem> itemList = material.getItems();
			if(itemList != null){
				for(MaterialItem item : itemList){
					MaterialArticle m = new MaterialArticle();
					if(item.getNewsItems() != null && item.getNewsItems().size() > 0){
						MaterialArticle ma = item.getNewsItems().get(0);//用第一个图文的简介、标题、作者、url
						m.setAuthor(ma.getAuthor());
						m.setTitle(ma.getTitle());
						m.setUrl(ma.getUrl());
					}
					materialList.add(m);
				}
			}
		}
		return getResult(materialArticle,materialList);
	}
	
	
	//上传图文素材
	@RequestMapping(value = "/doUploadMaterial")
	public  ModelAndView doUploadMaterial(MsgNews msgNews) throws Exception {
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		String rstMsg = "上传图文消息素材";
		List<MsgNews> msgNewsList = new ArrayList<MsgNews>();
		msgNewsList.add(msgNews);
		JSONObject rstObj = WxApiClient.uploadNews(msgNewsList, mpAccount);
		if(rstObj.containsKey("media_id")){
			ModelAndView mv = new ModelAndView("common/success");
			mv.addObject("successMsg", "上传图文素材成功,素材 media_id : " + rstObj.getString("media_id"));
			return mv;
		}else{
			rstMsg = ErrCode.errMsg(rstObj.getIntValue("errcode"));
		}
		ModelAndView mv = new ModelAndView("common/failure");
		mv.addObject("failureMsg", rstMsg);
		return mv;
	}
	
	//获取openid
	@RequestMapping(value = "/oauthOpenid.html")
	public ModelAndView oauthOpenid(HttpServletRequest request) throws WxErrorException {
		log.info("-------------------------------------oauthOpenid-----<0>-------------------");
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		log.info("-------------------------------------oauthOpenid-----<1>-------------------mpAccount："+mpAccount.getAccount());
		if(mpAccount != null){
			ModelAndView mv = new ModelAndView("wxweb/oauthopenid");
			//拦截器已经处理了缓存,这里直接取
			String openid = WxMemoryCacheClient.getOpenid(request.getSession().getId());
			log.info("-------------------------------------oauthOpenid-----<2>-------------------openid:"+openid);
			AccountFans fans = myService.syncAccountFans(openid,mpAccount,false);//同时更新数据库
			mv.addObject("openid", openid);
			mv.addObject("fans", fans);
			log.info("-------------------------------------oauthOpenid-----<3>-------------------fans:"+fans.getNicknameStr());
			return mv;
		}else{
			ModelAndView mv = new ModelAndView("common/failuremobile");
			mv.addObject("message", "OAuth获取openid失败");
			log.info("-------------------------------------oauthOpenid-----<4>-------------------");
			return mv;
		}
	}
	
	/**
	 * 生成二维码
	 * @param request
	 * @param num 二维码参数
	 * @return
	 */
	@RequestMapping(value = "/createQrcode", method = RequestMethod.POST)
	public ModelAndView createQrcode(HttpServletRequest request,Integer num) throws WxErrorException {
		ModelAndView mv = new ModelAndView("wxcms/qrcode");
		mv.addObject("cur_nav", "qrcode");
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		if(num != null){
			byte[] qrcode = WxApiClient.createQRCode(60,num,mpAccount);//有效期60s
			String url = UploadUtil.byteToImg(request.getServletContext().getRealPath("/"), qrcode);
			mv.addObject("qrcode", url);
		}
		mv.addObject("num", num);
		return mv;
	}
	
	//以根据openid群发文本消息为例
	@RequestMapping(value = "/massSendTextMsg", method = RequestMethod.POST)
	public void massSendTextMsg(HttpServletResponse response,String openid,String content) throws WxErrorException {
		content = "群发文本消息";
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		String rstMsg = "根据openid群发文本消息失败";
		if(mpAccount != null && !StringUtils.isBlank(openid)){
			List<String> openidList = new ArrayList<String>();
			openidList.add(openid);
			//根据openid群发文本消息
			JSONObject result = WxApiClient.massSendTextByOpenIds(openidList, content, mpAccount);
			try {
				if(result.getIntValue("errcode") != 0){
					response.getWriter().write("send failure");
				}else{
					response.getWriter().write("send success");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ModelAndView mv = new ModelAndView("common/failure");
		mv.addObject("failureMsg", rstMsg);
	}
	
	/**
	 * 发送客服消息
	 * @param openid ： 粉丝的openid
	 * @return
	 */
	@RequestMapping(value = "/sendCustomTextMsg", method = RequestMethod.POST)
	public void sendCustomTextMsg(HttpServletRequest request,HttpServletResponse response,String openid) throws WxErrorException {
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		String content = "微信派官方测试客服消息";
		JSONObject result = WxApiClient.sendCustomTextMessage(openid, content, mpAccount);
		try {
			if(result.getIntValue("errcode") != 0){
				response.getWriter().write("send failure");
			}else{
				response.getWriter().write("send success");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 微信异步返回
	 * @param requestBodyXml
	 * @return
	 */
	@RequestMapping(value = "/wxipay_noity")
	public @ResponseBody com.wxmp.core.util.wx.HTTPResultXml wxipay_noity(@RequestBody String requestBodyXml) {
		log.info("-------------------------------------wxipay_noity-----<0>-------------------requestBodyXml:"+requestBodyXml);		
		
		String jsonStr = requestBodyXml;
		Map<String, Object> dataMap = new HashMap<String, Object>();
	
		Map map = new HashMap();
		try {
			map = com.wxmp.core.util.wx.TenpayUtil2.doXMLParseByDom4j(jsonStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String return_code = (String) map.get("return_code");
		String return_msg = (String) map.get("return_msg");
		if("SUCCESS".equals(return_code)){
			String transaction_id = (String) map.get("transaction_id");
			String out_trade_no = (String) map.get("out_trade_no");
			
		    return_code="<![CDATA[SUCCESS]]>";
		    return_msg="<![CDATA[OK]]>";		
		}else{
			 return_msg= (String) map.get("err_code_des");
			 return_code="<![CDATA[FAIL]]>";
			 return_msg="<![CDATA["+return_msg+"]]>";	
		}		  
	
		
		com.wxmp.core.util.wx.HTTPResultXml httpResultXml=new com.wxmp.core.util.wx.HTTPResultXml();
		httpResultXml.setReturn_code(return_code);
		httpResultXml.setReturn_msg(return_msg);
		
		//String openid = WxMemoryCacheClient.getOpenid(request.getSession().getId());//先从缓存中获取openid
	
	/*	if(!array_key_exists("transaction_id", $data)){
			$msg = "输入参数不正确";
			return false;
		}
		//查询订单，判断订单真实性
		if(!$this->Queryorder($data["transaction_id"])){
			$msg = "订单查询失败";
			return false;
		}
		*/
		return httpResultXml;
	}

	
    /**
     * 给粉丝发送文本消息
     * @param msgId
     * @param openid
     * @return
     */
	@RequestMapping(value = "/sendTextMsgByOpenId", method = RequestMethod.POST)
	@ResponseBody
	public AjaxResult sendTextMsgByOpenId(String msgId, String openid) throws WxErrorException  {
		MsgText msgText = msgTextService.getById(msgId);
		String content = msgText.getContent();
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		JSONObject result = WxApiClient.sendCustomTextMessage(openid, content, mpAccount);

		if (result.getIntValue("errcode") != 0) {
			return AjaxResult.failure(result.toString());
		} else {
			return AjaxResult.success();
		}
	}

	/**
	 * 客服接口-发送图文消息
	 *
	 * @param id
	 * @param openid
	 * @return
	 */
	@RequestMapping(value = "/sendNewsByOpenId", method = RequestMethod.POST)
	@ResponseBody
	public AjaxResult sendNewsByOpenId(String id, String openid) throws WxErrorException {
		
		MsgNews msgNews  = this.msgNewsService.getById(id);

		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		JSONObject result = WxApiClient.sendCustomNews(openid, msgNews, mpAccount);
		log.info(" 客服接口-发送图文消息：" + result.toString());
		if (result.getIntValue("errcode") != 0) {
			return AjaxResult.failure(result.toString());
		} else {
			return AjaxResult.success();
		}
	}
	
    /**
     * 客服接口 -批量发送文本消息
     * @param textId
     * @param openIds
     * @return
     */
	@RequestMapping(value = "/batchSendText", method = RequestMethod.POST)
	@ResponseBody
	public String batchSendText(String textId,String openIds) throws WxErrorException {
		String code = "1";
		MsgText msgText = msgTextService.getById(textId);
		String[] openIdAarry = {"0"};
		if(openIds.contains(",")){
			openIdAarry = openIds.split(",");
		}else{
			openIdAarry[0] = openIds;
		}
		String content = msgText.getContent();
		
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		
		for(int i = 0; i<openIdAarry.length; i++){
			String openid = openIdAarry[i];
			JSONObject result = WxApiClient.sendCustomTextMessage(openid, content, mpAccount);
		}

		return code;
	}
	
	/**
     * 群发-文本消息
     * @param textId
     * @param openIds
     * @return
     */
	@RequestMapping(value = "/massSendTextByOpenIds", method = RequestMethod.POST)
	@ResponseBody
	public String massSendTextByOpenIds(String textId,String openIds) throws WxErrorException {
		String code = "0";
		MsgText msgText = msgTextService.getById(textId);
		//分隔字符串
		String[] openIdAarry = openIds.split(",");

		String content = msgText.getContent();
		
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		//openids
		List<String> openidList = new ArrayList<String>();
		for(int i = 0; i<openIdAarry.length; i++){
			String openid = openIdAarry[i];
			openidList.add(openid);
		}
		JSONObject result = WxApiClient.massSendTextByOpenIds(openidList,content,mpAccount);
		log.info(" 群发-文本消息："+result.toString());
		if(result.getIntValue("errcode") != 0){
			code = result.toString();//发送失败
		}else{
			code = "1";//发送成功
		}
		return code;
	}
	
	/**
	 * 高级群发-图文消息|
	 * @param newsId
	 * @param openIds
	 * @return
	 */
	@RequestMapping(value = "/massSendNewsByOpenIds", method = RequestMethod.POST)
	@ResponseBody
	public String massSendNewsByOpenIds(String newsId,String openIds) throws WxErrorException {
		String code = "";
        MsgNews msgNews = this.msgNewsService.getById(newsId);
		
		List<MsgNews> msgNewsList = new ArrayList<MsgNews>();
		msgNewsList.add(msgNews);
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		//先上传图文素材
		//JSONObject newsObject = WxApiClient.uploadNews(msgNewsList, mpAccount);
		//媒体id
        String media_id = msgNews.getMediaId();

		//分隔字符串
		String[] openIdAarry = openIds.split(",");
		//openids
		List<String> openidList = new ArrayList<String>();
		for(int i = 0; i<openIdAarry.length; i++){
			String openid = openIdAarry[i];
			openidList.add(openid);
		}
		JSONObject massQesultObj = WxApiClient.massSendByOpenIds(openidList,media_id,MsgType.MPNEWS,mpAccount);
		
		if(massQesultObj.getIntValue("errcode") != 0){
			code = massQesultObj.toString();
		}else{
			code = "1";//发送成功
		}
		return code;
	}
	

	/**
	 * 高级群发-图文消息|
	 * @param mediaId
	 * @param openIds
	 * @return
	 */
	@RequestMapping(value = "/sendMaterialByOpenIds", method = RequestMethod.POST)
	@ResponseBody
	public String sendMaterialByOpenIds(String mediaId,String openIds) throws WxErrorException {
		String code = "";
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		//分隔字符串
		String[] openIdAarry = openIds.split(",");
		//openids
		List<String> openidList = new ArrayList<String>();
		for(int i = 0; i<openIdAarry.length; i++){
			String openid = openIdAarry[i];
			openidList.add(openid);
		}
		JSONObject massQesultObj = WxApiClient.massSendByOpenIds(openidList,mediaId,MsgType.MPNEWS,mpAccount);
		
		if(massQesultObj.getIntValue("errcode") != 0){
			code = massQesultObj.toString();
		}else{
			code = "1";//发送成功
		}
		return code;
	}
	
	//创建微信公众账号菜单
	@RequestMapping(value = "/doPublishMenu")
	@ResponseBody
	public AjaxResult doPublishMenu() throws WxErrorException  {
		JSONObject rstObj = null;
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();
		if(mpAccount != null){
			rstObj = myService.publishMenu(mpAccount);
			if(rstObj != null){//成功，更新菜单组
				return AjaxResult.success();
//				if(rstObj.containsKey("menu_id")){
//					ModelAndView mv = new ModelAndView("common/success");
//					mv.addObject("successMsg", "创建菜单成功");
//					code = "1";
//					return code;
//				}else if(rstObj.containsKey("errcode") && rstObj.getIntValue("errcode") == 0){
//					ModelAndView mv = new ModelAndView("common/success");
//					mv.addObject("successMsg", "创建菜单成功");
//					code = "1";
//					return code;
//				}
			}
		}
		return AjaxResult.failure();
	}
	
	
	//删除微信公众账号菜单
	@RequestMapping(value = "/deletePublicMenu")
	@ResponseBody
	public String deletePublicMenu(HttpServletRequest request) throws WxErrorException  {
		String code = "";
		JSONObject rstObj = null;
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		if(mpAccount != null){
			rstObj = myService.deleteMenu(mpAccount);
			if(rstObj != null && rstObj.getIntValue("errcode") == 0){
				code = "1";
				return code;
			}
		}
		
		String failureMsg = "删除菜单失败";
		if(rstObj != null){
			failureMsg += ErrCode.errMsg(rstObj.getIntValue("errcode"));
		}
		code = failureMsg;
		return code;
	}
	/**
	 * 统计分析
	 * @param start 开始时间
	 * @param end 结束时间
	 * @return
	 * @throws WxErrorException
	 */
	@RequestMapping(value = "/dataCube")
	@ResponseBody
	public AjaxResult dataCube(String type,String start,String end) throws WxErrorException  {
		MpAccount mpAccount = WxMemoryCacheClient.getMpAccount();//获取缓存中的唯一账号
		String accessToken = WxApiClient.getAccessToken(mpAccount);
		JSONObject result = WxApi.forDataCube(accessToken, type, start, end);
		return AjaxResult.success(result);
	}

	/**
	 * 统计分析
	 * @return
	 * @throws WxErrorException
	 */
	@Resource
	private AccessTokenDao accessTokenDao;

	@RequestMapping(value = "/getAccessToken", method = RequestMethod.POST)
	@ResponseBody
	public String getAccessTokenFroUMPS(@RequestBody MpAccount mpAccount) throws WxErrorException  {
		// 获取唯一的accessToken，如果是多账号，请自行处理
		AccessToken token = null;
		// 获取当前的开始时间
		long tt = System.currentTimeMillis();
		//Calendar calendar = Calendar.getInstance();
		//int second = calendar.get(Calendar.SECOND);
		String time = String.valueOf(tt / 1000);
		// 查询是否存在此appid
		AccessTokens accessToken1 = accessTokenDao.getById(mpAccount.getAppid());
		// 新增存在appid有问题,去判断
		if (accessToken1 == null) {
			token = WxApi.getAccessToken(mpAccount.getAppid(), mpAccount.getAppsecret());
			if (token.getErrcode() == null) {
				AccessTokens accessToken3 = new AccessTokens(mpAccount.getAppid(), mpAccount.getAppsecret(),
						token.getAccessToken(),time);
				accessTokenDao.add(accessToken3);
				return token.getAccessToken();
			}
			return null;
		}
		long start = Long.parseLong(accessToken1.getCreateTime());
		long end = Long.parseLong(time);
		// 超过两小时，修改数据库调接口
		if ((end - start) >= 7200) {
			token = WxApi.getAccessToken(mpAccount.getAppid(), mpAccount.getAppsecret());
			AccessTokens accessToken3 = new AccessTokens(accessToken1.getId(),mpAccount.getAppid(), mpAccount.getAppsecret(),
					token.getAccessToken(),
					String.valueOf(end));
			accessTokenDao.update(accessToken3);
			return token.getAccessToken();
		} else {
			return accessToken1.getAccessToken();
		}
	}

	/**
	 *@Author 86151
	 *@Date 2020/12/28 10:26
	 *Description 微信服务/公众号调用,功能:{
     *                                       公众号：取消授权、允许授权、更新授权
     *                                       微信服务：验证票据
     *                                      }
	 * * @param  : null
	 * * @return : null
	 */
    private String encodingAesKey = "ndbBco26cAH3rBvDfFP0JmdCdxJ2AdqWbmQKr3gtwTB";

    private String messageCheckToken = "0c5cc9cebcc54c4ea1b2a7e1a00db569";

    private String componentAppId = "wx4d553967d6422132";

    private String componentAppSecret = "f0a6b4545311382164e96733e0f885ed";

	@PostMapping(value = "/ticket", produces = "text/xml;charset=utf-8")
	@ResponseBody
	public String getTicket(@RequestParam("timestamp") String timestamp, @RequestParam("nonce") String nonce, @RequestParam("msg_signature")String msgSignature, @RequestBody String postData)throws WxErrorException{
		log.debug("timestamp：" + timestamp);
		log.debug("nonce：" + nonce);
		log.debug("msgSignature：" + msgSignature);
		log.debug("postData：" + postData);

		// 微信文档问题：标签格式修改
		String postDataXML = postData.replaceAll("AppId","ToUserName");
		ComponentContent componentContent = null;
		try {
			log.debug(messageCheckToken + "," + encodingAesKey + "," + componentAppId);
		    // 解密类型aes
			WXBizMsgCrypt pc = new WXBizMsgCrypt(messageCheckToken, encodingAesKey, componentAppId);
			// 进行解密
			String decryData = pc.decryptMsg(msgSignature, timestamp, nonce, postDataXML);

			log.debug("输出日志：" + decryData);
			ConstantParseHandler constantParseHandler = new ConstantParseHandler();
			// 创建解析工厂
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // 创建解析
            SAXParser saxParser = saxParserFactory.newSAXParser();
            InputSource inputSource = new InputSource(new StringReader(decryData));

            // 解析成实体类
            saxParser.parse(inputSource, constantParseHandler);
			componentContent = constantParseHandler.getComponentContent();

            log.debug("查看ticket:" + componentContent.getComponentVerifyTicket());
            log.debug("查看创建时间:" + componentContent.getCreatTime());
            log.debug("查询类型:" + componentContent.getInfoType());

		} catch (SAXNotSupportedException e) {
            e.printStackTrace();
            return "fail";
        } catch (SAXNotRecognizedException e) {
            e.printStackTrace();
			return "fail";
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
			return "fail";
        } catch (SAXException e) {
            e.printStackTrace();
			return "fail";
        } catch (IOException e) {
            e.printStackTrace();
			return "fail";
        } catch (AesException e) {
			e.printStackTrace();
			return "fail";
		}
		log.debug("success");
		//myService.buildComponent_access_token(null, componentContent.getComponentVerifyTicket());
		return "success";
	}

	/**
	 *@Author 86151
	 *@Date 2020/12/29 10:27
	 *Description 获取第三方令牌
	 * * @param  : jsonObject
	 * * @return : java.lang.String
	 */
	@PostMapping(value = "/getcomponent_access_token")
	@ResponseBody
	public String getcomponent_access_token(@RequestBody JSONObject jsonObject)throws WxErrorException{
		String ticket = jsonObject.getString("ticket");
		String appId = jsonObject.getString("appId");

		String component_access_token = componentService.buildComponent_access_token(appId,ticket);
		return component_access_token;
	}

	/**
	 *@Author 86151
	 *@Date 2020/12/29 10:27
	 *Description 获取预授权码
	 * * @param  : jsonObject
	 * * @return : void
	 */
	@PostMapping(value = "/getPreAuthCode")
	@ResponseBody
	public void getPreAuthCode(@RequestBody JSONObject jsonObject){
		String component_access_token = jsonObject.getString("component_access_token");
		String component_appid = jsonObject.getString("component_appid");
		String getPreAuthCode = null;
		try {
			getPreAuthCode = componentService.getPreAuthCode(component_access_token, component_appid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("打印出成果的预授权码：" + getPreAuthCode);
	}

	/**
	 *@Author 86151
	 *@Date 2020/12/29 15:20
	 *Description 刷新令牌
	 * * @param  : jsonObject
	 * * @return : boolean
	 */
	@PostMapping(value = "/refreshtoken")
	@ResponseBody
	public boolean refreshtoken(@RequestBody JSONObject jsonObject) {
		String component_access_token = jsonObject.getString("component_access_token");
		String authorizer_appid = jsonObject.getString("authorizer_appid");
		boolean result = componentService.refreshtoken(component_access_token, authorizer_appid);
		return result;
	}

	/**
	 *@Author 86151
	 *@Date 2020/12/29 15:20
	 *Description 获取授权方的信息
	 * * @param  : jsonObject
	 * * @return : java.lang.String
	 */
	@PostMapping(value = "/getAuthInfo")
	@ResponseBody
	public String getAuthInfo(@RequestBody JSONObject jsonObject){
		String component_access_token = jsonObject.getString("component_access_token");
		String component_appid = jsonObject.getString("component_appid");
		String authorization_code = jsonObject.getString("authorization_code");
		try {
			componentService.selectAuthInfo(component_access_token, component_appid, authorization_code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 发送模板消息
	 * @return
	 */
	@RequestMapping(value = "/sendTemplateMessage", method = RequestMethod.POST)
	@ResponseBody
	public boolean sendTemplateMessage(@RequestBody JSONObject jsonObject) throws WxErrorException  {

		TemplateMessage tplMsg = new TemplateMessage();
		//公众号id
		String authorizer_appid = jsonObject.getString("authorizer_appid");
		//接收人openid
		String touser = jsonObject.getString("touser");
		//模板id
		String template_id = jsonObject.getString("template_id");

		//模板id
		String url = jsonObject.getString("url");

		HashMap data = (HashMap) jsonObject.get("data");


		tplMsg.setOpenid(touser);
		tplMsg.setTemplateId(template_id);
		tplMsg.setUrl(url);
		tplMsg.setData(data);


//		for (String openId : openIdArray) {
//			tplMsg.setOpenid(openId);
//			//微信公众号号的template id，开发者自行处理参数
//			tplMsg.setTemplateId("azx4q5sQjWUk1O3QY0MJSJkwePQmjR-T5rCyjyMUw8U");
//
//			tplMsg.setUrl("https://www.smartwx.info");
//			Map<String, String> dataMap = new HashMap<String, String>();
//			dataMap.put("first", "smartadmin管理后台已经上线，欢迎吐槽");
//			dataMap.put("keyword1", "时间：" + DateUtil.changeDateTOStr(new Date()));
//			dataMap.put("keyword2", "码云平台地址：https://gitee.com/qingfengtaizi/wxmp");
//			dataMap.put("keyword3", "github平台地址：https://github.com/qingfengtaizi/wxmp-web");
//			dataMap.put("remark", "我们期待您的加入");
//			tplMsg.setDataMap(dataMap);
		boolean result = componentService.sendTemplateMessage(tplMsg,authorizer_appid);

		return result;
	}
}
