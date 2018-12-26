package es.sidelab.webchat;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import es.codeurjc.webchat.Chat;
import es.codeurjc.webchat.ChatManager;
import es.codeurjc.webchat.User;

public class ChatManagerTest {

	@Test
	public void newChat() throws InterruptedException, TimeoutException {

		// Crear el chat Manager
		ChatManager chatManager = new ChatManager(5);

		// Crear un usuario que guarda en chatName el nombre del nuevo chat
		final String[] chatName = new String[1];

		chatManager.newUser(new TestUser("user") {
			public void newChat(Chat chat) {
				chatName[0] = chat.getName();
			}
		});

		// Crear un nuevo chat en el chatManager
		chatManager.newChat("Chat", 5, TimeUnit.SECONDS);

		// Comprobar que el chat recibido en el método 'newChat' se llama 'Chat'
		assertTrue("The method 'newChat' should be invoked with 'Chat', but the value is "
				+ chatName[0], Objects.equals(chatName[0], "Chat"));
	}

	@Test
	public void newUserInChat() throws InterruptedException, TimeoutException {

		ChatManager chatManager = new ChatManager(5);

		final String[] newUser = new String[1];

		TestUser user1 = new TestUser("user1") {
			@Override
			public void newUserInChat(Chat chat, User user) {
				newUser[0] = user.getName();
			}
		};

		TestUser user2 = new TestUser("user2");

		chatManager.newUser(user1);
		chatManager.newUser(user2);

		Chat chat = chatManager.newChat("Chat", 5, TimeUnit.SECONDS);

		chat.addUser(user1);
		chat.addUser(user2);

		assertTrue("Notified new user '" + newUser[0] + "' is not equal than user name 'user2'",
				"user2".equals(newUser[0]));

	}

	// Crear el chat Manager
	ChatManager chatManager = new ChatManager(50);

	@Test
	public void newConcurrentUserInChat() throws InterruptedException, TimeoutException {

		int concurrentUsers = 4;
		
		ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);

	    ArrayList<Future<String>> futures = new ArrayList<Future<String>>(concurrentUsers);
		try {

			CompletionService<String> completionService = new ExecutorCompletionService<>(
					executor);

			for (int i = 0; i < concurrentUsers; i++) {
				int userId = i;
	            futures.add(completionService.submit(() -> userTestBehaviour1(userId)));
			}

			for (int i = 0; i < concurrentUsers; i++) {

				Future<String> completedTask = completionService.take();

				System.out.println("Task: " + completedTask.get());
			}

		} 
		catch (ExecutionException ex) {
			System.out.println("!!!!!!! ERROR !!!!!!! " + ex.getMessage());
			ex.printStackTrace();
			assertTrue("Exception: "+ ex.getMessage() + " raised", false);
	         for (Future<String> f : futures)
	             f.cancel(true);			
		}
		finally {
			executor.shutdown();
		}
	}

	public String userTestBehaviour1(int userId) {

		final int N_ACTIONS = 5;
		
//		try {
//			Thread.sleep((long) (Math.random() * 5000));
//		} catch (InterruptedException e) {
//			return " !!!! ERROR !!!!! - Action with " + userId + " interrupted";
//		}

		try {
			
			TestUser user = new TestUser("user"+userId);

			chatManager.newUser(user);
		
			Chat chat = null;
			TestUser userInChatTestUser;

			for (int i = 0; i < N_ACTIONS; i++) {
				chat = chatManager.newChat("Chat"+i, 5, TimeUnit.SECONDS);
				chat.addUser(user);
				for (User userInChat : chat.getUsers()) {
					userInChatTestUser = (TestUser) userInChat;
					System.out.println("Action: "+ userId + " --- User: " + userInChatTestUser.getName() + " in chat: " + chat.getName());
				}

			}
			return "!!! GREAT !!! - Action with " + userId + " OK";
		
		} catch (InterruptedException | TimeoutException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			return " !!!! ERROR !!!!! - Action with " + userId + " interrupted: ";
		}
	}
}
