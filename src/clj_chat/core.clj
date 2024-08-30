(ns clj-chat.core
  (:require [clojure.java.io :as io])
  (:import [java.net ServerSocket InetSocketAddress]
           [java.nio.channels ServerSocketChannel Selector SelectionKey]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets])
  (:gen-class))


;; Global default values
(def ^:const port 10000)
(def ^:const buffer-size 10000)
(def ^:const clients (atom {}))


(defn accept-connection
  "Accepts the connection from the client and add the client state to
  client state atom"
  [server-channel selector]
  (let [client (.accept server-channel)]
    (.configureBlocking client false)
    (let [key (.register client selector SelectionKey/OP_READ)]
      (swap! clients assoc client key)
      (println "New client connected"))))


(defn buffer->String
  "Reads from a buffer and convert to string"
  [buffer]
  (String. (.array buffer) 0 (.position buffer) StandardCharsets/UTF_8))


(defn handle-cleint
  [key selector]
  (let [client (.channel key)
        buffer (ByteBuffer/allocate buffer-size)]
    (try
      (if (> (.read client buffer) 0)
        (let [message (buffer->String buffer)]
          (println (str "Received: " message)))))))


(defn start-server
  "Starts a chat server atp the default port, returns nil"
  [port]
  (let [selector (Selector/open)
        server-channel (ServerSocketChannel/open)]
    (.configureBlocking server-channel false)
    (.bind (.socket server-channel) (InetSocketAddress. port))
    (.register server-channel selector SelectionKey/OP_ACCEPT)
    (prn "Chat server started on port: " port)
    (while true
      (when (> (.select selector) 0)
        (let [selected-keys (.selectedKeys selector)]
          (doseq [key selected-keys]
            (cond
              ;; If the key is acceptable type, then accept connection
              (.isAcceptable key) (accept-connection! server-channel selector)
              ;; If the key is readable, then read from the client and
              ;; handle it
              (.isReadable key) (handle-client! key selector)))
          (.clear selected-keys))))))
