/*
 * Copyright (c) 2013 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.lib.io;

import com.datatorrent.lib.io.SmtpOutputOperator;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.icegreen.greenmail.util.ServerSetup;
import java.util.HashMap;
import java.util.Map;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.junit.Assert;
import org.junit.Test;

public class SmtpOutputOperatorTest
{
  @Test
  public void testSmtpOutputNode() throws Exception
  {

    String subject = "ALERT!";
    String content = "This is an SMTP operator test {}";
    String from = "jenkins@datatorrent.com";
    String to = "jenkins@datatorrent.com";

    GreenMail greenMail = new GreenMail(ServerSetupTest.ALL);
    greenMail.start();

    SmtpOutputOperator node = new SmtpOutputOperator();
    node.setFrom(from);
    node.addRecipient(SmtpOutputOperator.RecipientType.TO, to);
    node.setContent(content);
    node.setSmtpHost("127.0.0.1");
    node.setSmtpPort(ServerSetupTest.getPortOffset() + ServerSetup.SMTP.getPort());
    node.setSmtpUserName(from);
    node.setSmtpPassword("<password>");
    //node.setUseSsl(true);
    node.setSubject(subject);

    node.setup(null);

    Map<String, String> data = new HashMap<String, String>();
    data.put("alertkey", "alertvalue");
    node.beginWindow(1000);
    node.input.process(data);
    node.endWindow();
    Assert.assertTrue(greenMail.waitForIncomingEmail(5000, 1));
    MimeMessage[] messages = greenMail.getReceivedMessages();
    Assert.assertEquals(1, messages.length);
    String receivedContent = messages[0].getContent().toString().trim();
    String expectedContent = content.replace("{}", data.toString()).trim();

    Assert.assertTrue(expectedContent.equals(receivedContent));
    Assert.assertEquals(from, ((InternetAddress)messages[0].getFrom()[0]).getAddress());
    Assert.assertEquals(to, messages[0].getAllRecipients()[0].toString());
    node.teardown();
    greenMail.stop();
  }

}
