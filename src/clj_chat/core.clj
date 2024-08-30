(ns clj-chat.core
  (:require [clojure.java.io :as io])
  (:import [java.net ServerSocket InetSocketAddress]
           [java.nio.channels ServerSocketChannel Selector SelectionKey]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets])
  (:gen-class))


(def port 10000)
(def buffer-size 1024)

;; We store all the client in an atom
(def clients (atom {}))


(defn send-message!
  [client message]
  (let [buffer (ByteBuffer/wrap (.getBytes message StandardCharsets/UTF_8))]
    (.write client buffer)
    (.flip buffer)))


(defn broadcast!
  [sender message]
  (let [sender-name (get-in @clients [sender :name])
        formatted-message (str sender-name ": " message)]
    (doseq [[client _] @clients]
      (when (not= client sender)
        (send-message! client formatted-message)))))


(defn handle-client!
  [key selector]
  (let [client (.channel key)
        buffer (ByteBuffer/allocate buffer-size)]
    (try
      (if (> (.read client buffer) 0)
        (let [message (String. (.array buffer) 0 (.position buffer) StandardCharsets/UTF_8)]
          (println (str "Received: " message))
          (if-let [name (get-in @clients [client :name])]
            (broadcast! client (str/trim message))
            (do
              (swap! clients assoc-in [client :name] (str/trim message))
              (println (str "New client named: " (str/trim message)))
              (send-message! client "Welcome to the chat server! You can now start sending messages."))))
        (do
          (println (str "Client " (get-in @clients [client :name]) " disconnected"))
          (swap! clients dissoc client)
          (.cancel key)
          (.close client)))
      (catch Exception e
        (println (str "Error handling client: " (.getMessage e)))
        (swap! clients dissoc client)
        (.cancel key)
        (.close client)))))


(defn accept-connection!
  [server-channel selector]
  (let [client (.accept server-channel)]
    (.configureBlocking client false)
    (let [key (.register client selector SelectionKey/OP_READ)]
      (swap! clients assoc client {:key key})
      (println "New client connected")
      (send-message! client "Welcome! Please enter your name:"))))


(defn start-server!
  []
  (let [selector (Selector/open)
        server-channel (ServerSocketChannel/open)]
    (.configureBlocking server-channel false)
    (.bind (.socket server-channel) (InetSocketAddress. port))
    (.register server-channel selector SelectionKey/OP_ACCEPT)
    (println (str "Chat server started on port " port))
    (while true
      (when (> (.select selector) 0)
        (let [selected-keys (.selectedKeys selector)]
          (doseq [key selected-keys]
            (cond
              (.isAcceptable key) (accept-connection! server-channel selector)
              (.isReadable key) (handle-client! key selector)))
          (.clear selected-keys))))))


(defn -main [& args]
  (start-server!))
