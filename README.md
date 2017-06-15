<div class="header">
  <h1 style="text-align: center;">
    Distributed Systems<br />
  </h1>
  <h1 style="text-align: center;">
    Project - phase 2<br />
  </h1>
  <h2 style="text-align: center;">
    Engenharia Informática
  </h2>
  <h3 style="text-align: center;">
    Ano lectivo: 2016/2017, 2º Semestre
  </h3>
</div>
<h1>
  Index
</h1>
<div style="margin-left: 40px;">
  <span style="font-weight: bold;">Deadline</span>: May, 26th,
  23h59<br style="font-weight: bold;" />
  <span style="font-weight: bold;">Report deadline</span>: May,
  29th, in the DI secretary<br />
</div>
<p>
  <br />
</p>
<ul>
  <li>
    <a href="#objectivo">Goal</a>
  </li>
  <li>
    <strong><a href="#base1">Features</a></strong>
  </li>
  <li>
    <a href="#ambiente">Environment<br /></a>
  </li>
  <li>
    <a href="#report">Written Report<br /></a>
  </li>
  <li>
    <a href="#delivery">Delivery<br /></a>
  </li>
</ul>
<h1>
  <a name="objectivo" id="objectivo"></a>Goal
</h1>
<p>
  The goal of the second phase of the project is to enhance the
  system for indexing and searching information about (external)
  documents, by supporting the following features:
</p>
<ul>
  <li>Search results will include information for documents stored
  in third-party services;
  </li>
  <li>The system must be secure;
  </li>
  <li>The system must be fault-tolerant; and<br />
  </li>
  <li>
    <strong>Search results must reflect all documents added to the
    system, regardless to which server they were added
    to.</strong><br />
  </li>
</ul>
<h1>
  <a name="base1" id="base1"></a>Functionalities<br />
</h1>
<h2>
  Searching in third-party services [6.5 points]
</h2>
<p>
  <span style="font-weight: normal;">Your system must efficiently
  return results obtained by searching a third-party service
  efficiently.<br /></span>
</p>
<p>
  <span style="font-weight: normal;"><span style=
  "font-weight: bold;">Suggestion</span>: Create a</span> server
  (either in REST or SOAP) that acts as a proxy to a third-party
  service (e.g. Twitter). This proxy will allow clients to get
  results from documents stored in the third party service -- the
  only API function that will interact with the third-party service
  is the search.
</p>
<p>
  <span style="font-weight: bold;">Suggestion</span>: To make the
  search function efficient, try to minimize the number of queries
  issued in the third-party service by implementing a cache in the
  server.<br />
</p>
<p>
  <span style="font-weight: bold;">NOTE</span>: the proxy should
  access the general REST or SOAP APIs provided by the services
  directly, i.e., you should not use any client library provided by
  these services.<br />
</p>
<p>
  <br />
</p>
<p>
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp; <font color=
  "#CC0000"><strong>A search operation in a proxy only needs to
  return results found locally, either already in a cache or by
  performing que query in the third-party service.</strong></font>
</p>
<h2 style="margin-left: 40px;">
  <a href=
  "trab2-sd1-1617.html#Tracking_Causality_"><font><font color=
  "#990000">ADDENDUM [19/06]</font></font></a>
</h2>
<p>
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;&nbsp; <font color=
  "#CC0000"><strong>Other operations should return code 403:
  FORBIDDEN in rest and false in soap.</strong></font>
</p>
<h2>
  Security [3.5 points]
</h2>
<p>
  <span style="font-weight: normal;">Communications between the
  clients and the servers and among servers must be secure, using
  secure channels TLS/HTTPS).<br />
  Since TLS/HTTPS will only authenticate the server, some provision
  should be taken to authenticate the clients when adding or
  removing a document. For instance, one possible solution is to
  have a shared password among clients and servers.<br /></span>
</p>
<h2>
  Fault-tolerance<br />
</h2>
<p>
  <span style="font-weight: normal;">The system developed in the
  first phase is not fault-tolerant because: (1) the system relies
  on the rendezvous server for being able to operate; (2) if an
  indexing server fails, the information stored in the indexing
  server is (permanently) lost.</span>
</p>
<p>
  <span style="font-weight: normal;">To make the system
  fault-tolerant, you must replicate the information maintained by
  your servers.&nbsp;</span>
</p>
<p>
  <span style="font-weight: normal;">Fault-tolerance can be added
  to the system by developing a custom replication protocol.
  Alternatively, it will be possible to leverage any of the
  following third-party services:<br /></span>
</p>
<ul>
  <li>
    <span style="font-weight: normal;"><a href=
    "https://zookeeper.apache.org/">Zookeeper</a> - a highly
    reliable service for distributed coordination;</span>
  </li>
  <li>
    <span style="font-weight: normal;"><a href=
    "https://kafka.apache.org/">Apache Kafka</a> - a
    fault-tolerant, reliable, publish/subscribe service;</span>
  </li>
  <li>
    <span style="font-weight: normal;"><a href=
    "https://www.mongodb.com/">MongoDB</a> - a document-oriented
    NoSQL database.</span><br />
  </li>
</ul>
<p>
  <span style="font-weight: normal;">In order to use any of the
  third-party services listed above, a docker image will be
  provided to deploy a 3-node cluster with those services ready to
  use.<br />
  Once the services are deployed, your servers can act as clients
  to these services, using the following hostnames: zoo[1,2,3],
  kafka[1,2,3], mongo[1,2,3]. The servers are available at their
  default ports.</span>
</p>
<p>
  <span style="font-weight: normal;">The following approaches can
  be implemented (you should implement a single solution for each
  of the services -- the second replication service implemented is
  valued in 2 points, independently of the approach):</span>
</p>
<h2 style="margin-left: 40px;">
  Fault-tolerance: full replication with replicated log based on a
  reliable publish/subscribe system [1st service: 6 points]<br />
</h2>
<div style="margin-left: 40px;">
  It is possible to replicate the state of a server by executing
  the same sequence of operations in other server(s).<br />
  A reliable publish/subscribe system allows a publisher to
  reliably disseminate a sequence of messages (e.g. operations) to
  multiple subscribers. Some systems can persistently store
  messages to allow the delivery of messages to latecomers.<br />
  <br />
  <span style="font-weight: bold;">Suggestion</span>: use a
  reliable publish/subscribe system, e.g. Apache Kafka, to
  disseminate operations that modify the state of a server to all
  servers.<br />
  <br />
  <span style="font-weight: bold;">NOTE</span>: for efficiently
  recovering the state of a server when it restarts, you must
  periodically create a snapshot of the server state (otherwise,
  you need to replay all operations).<br />
</div>
<h2 style="margin-left: 40px;">
  <a href="#Tracking_Causality_"><font color="#990000">ADDENDUM
  [15/06]</font></a><br />
</h2>
<h2 style="margin-left: 40px;">
  Fault-tolerance: partial replication with replicated log based on
  a reliable publish/subscribe system [1st service: 7 points]<br />
</h2>
<div style="margin-left: 40px;">
  Enhance the previous solution for indexing servers by not storing
  all information in all servers.<br />
  <br />
  <span style="font-weight: bold;">NOTE</span>: this solution
  requires some search operation to be forwarded to a server that
  can handle them.<br />
</div>
<h2 style="margin-left: 40px;">
  <a href="#Tracking_Causality_"><font color="#990000">ADDENDUM
  [15/06]</font></a><br />
</h2>
<h2 style="margin-left: 40px;">
  Fault-tolerance: replication using third-party replicated storage
  system [1st service: 6 points]
</h2>
<div style="margin-left: 40px;">
  In the 3-tier architecture commonly used in Internet services,
  the application server often relies on a distributed storage
  service that stores the application state. In this case, the
  application servers maintain no state and access the storage
  service for handling client requests.<br />
  Implement such model in your system, by storing the information
  previously maintained by the servers in a distributed storage
  system, such as Zookeeper or MongoDB. Note that Zookeeper is
  typically used for storing small-sized and strongly consistent
  data used for naming and coordination, while MongoDB is a
  general-purpose database.<br />
  <br />
  <strong><font color="#990000">Note: ALL DATA stored in ZooKeeper
  should be placed under /sd [eg.
  /sd/rendezvous/id1]</font></strong><font color=
  "#990000"><font color="#000000">. This is necessary to allow the
  tester to clean zookeeper without stopping in rerunning the
  services...<br /></font></font> &nbsp;<strong><font color=
  "#990000"><br /></font></strong>
</div>
<h2 style="margin-left: 40px;">
  Fault-tolerance: implement replication yourself [1st service: 8
  points]
</h2>
<div style="margin-left: 40px;">
  Add replication to your system by implementing a replication
  protocol yourself.<br />
  <br />
  <span style="font-weight: bold;">NOTE</span>: replication
  protocols will be discussed in lecture in the week May,
  8-12.<br />
</div>
<h2 style="margin-left: 40px;">
  <a href="#Tracking_Causality_"><font color="#990000">ADDENDUM
  [15/06]</font></a>
</h2>
<h1>
  Rendezvous Server Interface<br />
</h1>
<p>
  To support the new features, the REST interface of the rendexvous
  service must be the following:
</p>
<p style=
"background-color: rgb(255, 255, 204); margin-left: 40px;">
  <code>/**<br />
  &nbsp;* Interface do servidor que mantem lista de
  servidores.<br />
  &nbsp;*/<br />
  public interface RendezVousAPI {<br />
  <br />
  &nbsp;&nbsp;&nbsp; /**<br />
  &nbsp;&nbsp;&nbsp; &nbsp;* Devolve array com a lista de
  servidores registados.<br />
  &nbsp;&nbsp;&nbsp; &nbsp;*/<br />
  &nbsp;&nbsp;&nbsp; @GET<br />
  &nbsp;&nbsp;&nbsp; @Produces(MediaType.APPLICATION_JSON)<br />
  &nbsp;&nbsp;&nbsp; public Endpoint[] endpoints();<br />
  <br />
  &nbsp;&nbsp;&nbsp; /**<br />
  &nbsp;&nbsp;&nbsp; &nbsp;* Regista novo servidor.<br />
  &nbsp;&nbsp;&nbsp; &nbsp;*/<br />
  &nbsp;&nbsp;&nbsp; @POST<br />
  &nbsp;&nbsp;&nbsp; @Path("/{id}")<br />
  &nbsp;&nbsp;&nbsp; @Consumes(MediaType.APPLICATION_JSON)<br />
  &nbsp;&nbsp;&nbsp; public void register( @PathParam("id") String
  id, @QueryParam("secret") String secret, Endpoint
  endpoint);<br />
  <br />
  &nbsp;&nbsp;&nbsp; /**<br />
  &nbsp;&nbsp;&nbsp; &nbsp;* De-regista servidor, dado o seu
  id.<br />
  &nbsp;&nbsp;&nbsp; &nbsp;*/<br />
  &nbsp;&nbsp;&nbsp; @DELETE<br />
  &nbsp;&nbsp;&nbsp; @Path("/{id}")<br />
  &nbsp;&nbsp;&nbsp; public void unregister(@PathParam("id") String
  id, @QueryParam("secret") String secret);<br />
  }<br />
  <br /></code><br />
</p>
<h1>
  Indexer Server Interface<br />
</h1>
<p>
  To support the new features, the REST interface of the indexing
  service must be the following (<a href=
  "ServerConfig.java">ServerConfig class available here</a>):
</p>
<p style=
"background-color: rgb(255, 255, 204); margin-left: 40px;">
  <code>@Path("/indexer")<br />
  <strong>public</strong> <strong>interface</strong> IndexerService
  {<br />
  <br /></code> <code>&nbsp;&nbsp;&nbsp; @GET<br />
  &nbsp;&nbsp;&nbsp; @Path("/search")<br />
  &nbsp;&nbsp;&nbsp; @Produces(MediaType.APPLICATION_JSON)<br />
  &nbsp;&nbsp;&nbsp; List&lt;String&gt; search(
  @QueryParam("query") String keywords );<br />
  <br /></code> <code>&nbsp;&nbsp;&nbsp; /**<br />
  &nbsp;&nbsp;&nbsp;&nbsp; * Configure server for accessing
  third-party service. Servers that do not act as a proxy should do
  nothing and return 200.<br />
  &nbsp;&nbsp;&nbsp;&nbsp; *<br /></code>
  <code>&nbsp;&nbsp;&nbsp;&nbsp; * secret: for protecting access to
  this function passed as a query parameter<br /></code>
  <code>&nbsp;&nbsp;&nbsp;&nbsp; * config: configuration for remote
  access<br /></code> <code>&nbsp;&nbsp;&nbsp;&nbsp; * Should
  return HTTP code 403 on security issue.<br />
  &nbsp;&nbsp;&nbsp;&nbsp; */<br /></code> <code>&nbsp;&nbsp;&nbsp;
  @PUT<br />
  &nbsp;&nbsp;&nbsp; @Path("/configure")<br /></code>
  <code>&nbsp;&nbsp;&nbsp;
  @Consumes(MediaType.APPLICATION_JSON)<br /></code>
  <code>&nbsp;&nbsp;&nbsp; void configure(</code>
  <code>@QueryParam("secret") String secret,</code>
  <code>ServerConfig config</code><code>);<br />
  <br /></code> <code>&nbsp;&nbsp;&nbsp; /**<br />
  &nbsp;&nbsp;&nbsp;&nbsp; * Secret for protecting access to this
  function passed as a query parameter<br />
  &nbsp;&nbsp;&nbsp;&nbsp; * Should return HTTP code 403 on
  security issue.<br />
  &nbsp;&nbsp;&nbsp;&nbsp; */<br />
  &nbsp;&nbsp;&nbsp; @POST<br />
  &nbsp;&nbsp;&nbsp; @Path("/{id}")<br />
  &nbsp;&nbsp;&nbsp; @Consumes(MediaType.APPLICATION_JSON)<br />
  <strong>&nbsp;&nbsp;&nbsp; void</strong> add( @PathParam("id")
  String id,</code> <code>@QueryParam("secret") String
  secret,</code> <code>Document doc );<br />
  <br /></code> <code>&nbsp;&nbsp;&nbsp; /**<br />
  &nbsp;&nbsp;&nbsp;&nbsp; * Secret for protecting access to this
  function passed as a query parameter<br />
  &nbsp;&nbsp;&nbsp;&nbsp; * Should return HTTP code 403 on
  security issue.<br />
  &nbsp;&nbsp;&nbsp;&nbsp; */<br /></code> <code>&nbsp;&nbsp;&nbsp;
  @DELETE<br />
  &nbsp;&nbsp;&nbsp; @Path("/{id}")<br />
  <strong>&nbsp;&nbsp;&nbsp; void</strong> remove( @PathParam("id")
  String id,</code> <code>@QueryParam("secret") String
  secret</code> <code>);<br />
  <br />
  }</code><br />
</p>
<p>
  &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; The SOAP interface of this
  server, should be the following:
</p>
<p style=
"background-color: rgb(255, 255, 204); margin-left: 40px;">
  <font face=
  "Courier New, Courier, monospace"><strong>package</strong>
  api.soap;<br />
  &nbsp;<br />
  @WebService<br />
  <strong>public</strong> <strong>interface</strong> IndexerService
  {<br />
  &nbsp;&nbsp;&nbsp; <strong><br />
  &nbsp;&nbsp;&nbsp;</strong>
  <strong>@WebFault</strong><strong><font color="#990000"><br />
  &nbsp;&nbsp;&nbsp; <font color="#000000">class <font color=
  "#660000">InvalidArgumentException</font></font></font></strong>
  <font color="#990000"><font color=
  "#000000"><strong>extends</strong> Exception</font></font>
  <strong><font color="#990000"><font color="#000000">{<br />
  <br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;</font></font></strong>
  <font color="#990000"><font color=
  "#000000"><strong>private</strong> <strong>static</strong>
  <strong>final</strong> <strong>long</strong> serialVersionUID =
  1L;<br /></font></font></font><br />
  <font face="Courier New, Courier, monospace"><font color=
  "#990000"><font color="#000000"><font face=
  "Courier New, Courier, monospace"><font color=
  "#990000"><font color="#000000">&nbsp;&nbsp;&nbsp;
  &nbsp;&nbsp;&nbsp; <strong>public</strong>
  InvalidArgumentException() {<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
  <strong>super</strong>("");<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
  }</font></font></font>&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; <strong>public</strong>
  InvalidArgumentException(String msg) {<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
  <strong>super</strong>(msg);<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; }<br /></font></font>
  <strong><font color="#990000"><font color=
  "#000000">&nbsp;&nbsp;&nbsp; }</font></font><br />
  <br /></strong></font> <font face=
  "Courier New, Courier, monospace"><strong>&nbsp;&nbsp;&nbsp;</strong>
  <strong>@WebFault</strong><strong><font color="#990000"><br />
  &nbsp;&nbsp;&nbsp; <font color="#000000">class <font color=
  "#660000">SecurityException</font></font></font></strong>
  <font color="#990000"><font color=
  "#000000"><strong>extends</strong> Exception</font></font>
  <strong><font color="#990000"><font color="#000000">{<br />
  <br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;</font></font></strong>
  <font color="#990000"><font color=
  "#000000"><strong>private</strong> <strong>static</strong>
  <strong>final</strong> <strong>long</strong> serialVersionUID =
  1L;<br /></font></font></font><br />
  <font face="Courier New, Courier, monospace"><font color=
  "#990000"><font color="#000000"><font face=
  "Courier New, Courier, monospace"><font color=
  "#990000"><font color="#000000">&nbsp;&nbsp;&nbsp;
  &nbsp;&nbsp;&nbsp; <strong>public</strong> SecurityException()
  {<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
  <strong>super</strong>("");<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
  }</font></font></font>&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; <strong>public</strong>
  SecurityException(String msg) {<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
  <strong>super</strong>(msg);<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; }<br /></font></font>
  <strong><font color="#990000"><font color=
  "#000000">&nbsp;&nbsp;&nbsp; }</font></font><br />
  <br /></strong></font> <font face=
  "Courier New, Courier, monospace"><strong>&nbsp;&nbsp;&nbsp;
  static</strong> <strong>final</strong> String
  NAME="IndexerService";<br />
  &nbsp;&nbsp;&nbsp; <strong>static</strong> <strong>final</strong>
  String NAMESPACE="http://sd2017";<br />
  &nbsp;&nbsp;&nbsp; <strong>static</strong> <strong>final</strong>
  String INTERFACE="api.soap.IndexerService";<br />
  <br />
  &nbsp;&nbsp;&nbsp; /* keywords contains a list of works separated
  by '+'<br />
  &nbsp;&nbsp;&nbsp; &nbsp;* returns the list of urls of the
  documents stored in this server that contain all the
  keywords<br />
  &nbsp;&nbsp;&nbsp; &nbsp;* throws IllegalArgumentException if
  keywords is null<br /></font> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;&nbsp; *
  throws SecurityException on security problem<br /></font>
  <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;&nbsp;
  */<br />
  &nbsp;&nbsp;&nbsp; @WebMethod<br />
  &nbsp;&nbsp;&nbsp; List&lt;String&gt; search(String keywords)
  <strong>throws</strong> <strong><font color=
  "#990000">InvalidArgumentException</font></strong></font>
  <font face="Courier New, Courier, monospace">;<br />
  <br /></font> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;
  /*<br /></font> <code>&nbsp;&nbsp;&nbsp;&nbsp; * secret: for
  protecting access to this function passed as a query
  parameter<br /></code> <code>&nbsp;&nbsp;&nbsp;&nbsp; * config:
  configuration for remote access<br /></code> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;&nbsp; *
  throws IllegalArgumentException if some parameter is
  null<br /></font> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp; &nbsp;*
  throws SecurityException on security problem<br /></font>
  <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;&nbsp;
  */<br />
  &nbsp;&nbsp;&nbsp; @WebMethod<br />
  &nbsp;&nbsp;&nbsp; void configure(</font><font face=
  "Courier New, Courier, monospace">String
  secret</font><code>,</code> <code>ServerConfig
  config</code><font face="Courier New, Courier, monospace">)
  <strong>throws</strong></font> <font face=
  "Courier New, Courier, monospace"><strong><font face=
  "Courier New, Courier, monospace"><font color=
  "#660000">InvalidArgumentException</font></font></strong></font><font face="Courier New, Courier, monospace"><strong><font color="#990000">,</font></strong></font>
  <font face="Courier New, Courier, monospace"><strong><font color=
  "#990000">SecurityException</font></strong></font> <font face=
  "Courier New, Courier, monospace">;<br />
  <br /></font> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp; /*<br />
  &nbsp;&nbsp;&nbsp;&nbsp; *</font> <code>secret protects access to
  this function.<br /></code> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;&nbsp; *
  return true if document was added, false if the document already
  exists in this server.<br />
  &nbsp;&nbsp;&nbsp; &nbsp;* throws IllegalArgumentException if doc
  is null<br /></font> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp; &nbsp;*
  throws SecurityException on security problem<br /></font>
  <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;&nbsp;
  */<br />
  &nbsp;&nbsp;&nbsp; @WebMethod<br />
  &nbsp;&nbsp;&nbsp; <strong>boolean</strong> add(Document
  doc</font><font face="Courier New, Courier, monospace">, String
  secret</font><font face="Courier New, Courier, monospace">)
  <strong>throws</strong></font> <font face=
  "Courier New, Courier, monospace"><strong><font face=
  "Courier New, Courier, monospace"><font color=
  "#660000">InvalidArgumentException</font></font></strong></font><font face="Courier New, Courier, monospace"><strong><font color="#990000">,</font></strong></font>
  <font face="Courier New, Courier, monospace"><strong><font color=
  "#990000">SecurityException</font></strong></font> <font face=
  "Courier New, Courier, monospace">;<br />
  <br />
  &nbsp;&nbsp;&nbsp; /*<br /></font> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;&nbsp;
  *</font> <code>secret protects access to this
  function.<br /></code> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;&nbsp; *
  return true if document was removed, false if was not found in
  the system.<br />
  &nbsp;&nbsp;&nbsp; &nbsp;* throws IllegalArgumentException if id
  is null<br /></font> <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp; &nbsp;*
  throws SecurityException on security problem<br /></font>
  <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;&nbsp;
  */<br />
  &nbsp;&nbsp;&nbsp; @WebMethod<br />
  &nbsp;&nbsp;&nbsp; <strong>boolean</strong> remove(String id,
  String secret) <strong>throws</strong></font> <font face=
  "Courier New, Courier, monospace"><strong><font face=
  "Courier New, Courier, monospace"><font color=
  "#660000">InvalidArgumentException</font></font></strong></font><font face="Courier New, Courier, monospace"><strong><font color="#990000">,</font></strong></font>
  <font face="Courier New, Courier, monospace"><strong><font color=
  "#990000">SecurityException</font></strong></font> <font face=
  "Courier New, Courier, monospace">;<br />
  }</font><br />
</p>
<p>
  <br />
</p>
<h1>
  Miscellaneous&nbsp;<br />
</h1>
<h2>
  Security: trust store<br />
</h2>
<p>
  When setting the trust store using the java parameter
  -Djavax.net.ssl.trustStore, the default trust store for Java is
  not used anymore. The implication of this is that certificates
  issued by known certification authorities stop being accepted by
  your program -- e.g. when accessing Twitter, the access fails
  because Twitter certificate is not accepted.<br />
  To solve this, you should add the needed client certificates to a
  trust store that already contains the certificates from known
  certification authorities. To this end, add your certificates to
  this <a href="client-base.jks">client-base.jks</a> trust store
  (instead of creating one from scratch) --- to do this, just use
  the keytool command as explained in TLS slides (the password for
  this keystore is <span style=
  "font-weight: bold;">changeit</span>):&nbsp; e.g.: keytool
  -importcert -keystore client-base.jks -alias server -file
  x.cert<br />
</p>
<h2>
  Query parameters for search
</h2>
<p>
  When sending a query parameter of the type "a+b" using the
  WebTarget client and when sending the same parameter in the URL
  from a browser, the URL is encoded differently (and received
  differently in the server).<br />
  To safely split the received string, you should use
  <strong>.split("[ \\+]")</strong> function in the server (note
  the blank space in the regex string parameter).
</p>
<h2>
  <a name="Tracking_Causality_" id=
  "Tracking_Causality_"></a>Tracking Causality&nbsp;<br />
</h2>
<p>
  Ideally, the replicated indexer service should provide causal
  consistency. Namely, it should guarantee that searches reflect
  the add/remove operations that happened before them.<br />
  Suppose the client adds a document <em>d</em> to an indexer named
  <em>i1</em> and <strong>then</strong> performs a search operation
  on an different indexer <em>i2</em>. If <em>d</em> matches the
  query then the search results returned by <em>i2</em> should
  include <em>d</em>.
</p>
<p>
  To track causality between requests, the indexer service can
  include in its replies to the client an HTTP HEADER
  "<strong>X-indexer-metadata</strong>"&nbsp; that the client will
  copy and include, unaltered, in subsequent
</p>
<p>
  requests. The value of the "<strong>X-indexer-metadata</strong>"
  header is opaque to the client. The indexer service can encode
  any information it requires to track requests from the same
  client among the<br />
  its servers. It allows, for instance, to inform an indexer
  <em>i2</em> that the client performed a previous add/remove
  operation in <em>i1</em>.<br />
</p>
<p>
  <br />
  In Jersey, the implementation method of a REST operation can
  obtain the value of a given header with the @HeaderParam
  annotation, like so:
</p>
<p>
  <code>&nbsp;&nbsp;&nbsp; @GET<br />
  &nbsp;&nbsp;&nbsp; @Path("/search")<br />
  &nbsp;&nbsp;&nbsp; @Produces(MediaType.APPLICATION_JSON)<br />
  &nbsp;&nbsp;&nbsp; List&lt;String&gt; search(
  @HeaderParam("</code><code><strong>X-indexer-metadata</strong>")
  String meta, @QueryParam("query") String keywords );</code>
</p>
<p>
  <br />
  To include an header in the reply to a given request, the
  operation can return a Response, like so:<br />
</p>
<p>
  <br />
  <code>&nbsp;&nbsp;&nbsp; @POST<br />
  &nbsp;&nbsp;&nbsp; @Path("/{id}")<br />
  &nbsp;&nbsp;&nbsp; @Consumes(MediaType.APPLICATION_JSON)<br />
  <strong>&nbsp;&nbsp;&nbsp;</strong> Response add(</code>
  <code><code>@HeaderParam("sd17-meta") String meta,</code>
  @PathParam("id") String id,</code> <code>@QueryParam("secret")
  String secret,</code> <code>Document doc ) {<br /></code>
</p>
<p>
  <code>&nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; ...<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; return
  Response.status(...).header("</code><code><strong>X-indexer-metadata</strong>",
  "&lt;string value&gt;").build();<br /></code>
</p>
<p>
  <code>&nbsp;&nbsp;&nbsp; }<br /></code><br />
</p>
<p>
  For Java WS webservices, the same can be achieved using the
  following pieces of code. Note that in Java WS, the value of an
  header is a list of strings,<br />
  rather than a single string value. In both cases, the tester will
  return whatever it receives.<br />
</p>
<p>
  <br />
  &nbsp;&nbsp;&nbsp; Include the following annotation in your
  service implementation.<br />
</p>
<p>
  <font face="Courier New, Courier, monospace"><br />
  &nbsp;&nbsp;&nbsp;
  <strong>@</strong><strong>Resource</strong><br />
  &nbsp;&nbsp;&nbsp; WebServiceContext wsCtx;<br /></font>
</p>
<p>
  <font face="Courier New, Courier, monospace"><br />
  &nbsp;&nbsp;&nbsp; <strong>private</strong> <strong>void</strong>
  setHeader(String key, <strong>Object</strong> value) {<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; MessageContext msgCtxt =
  wsCtx.getMessageContext();<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; Headers headers = (Headers)
  msgCtxt.get(MessageContext.HTTP_RESPONSE_HEADERS);<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; headers.put(key,
  Arrays.asList(value.toString()));<br />
  &nbsp;&nbsp;&nbsp; }<br /></font>
</p>
<p>
  <font face=
  "Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;<br /></font>
</p>
<p>
  <font face="Courier New, Courier, monospace">&nbsp;&nbsp;&nbsp;
  @SuppressWarnings("unchecked")<br />
  &nbsp;&nbsp;&nbsp; <strong>private</strong> &lt;T&gt; T
  getHeader( String key ) {<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; MessageContext msgCtxt =
  wsCtx.getMessageContext();<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; Map&lt;String,
  List&lt;String&gt;&gt; headers = (Map&lt;String,
  List&lt;String&gt;&gt;)
  msgCtxt.get(MessageContext.HTTP_REQUEST_HEADERS);<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; List&lt;String&gt;
  valueList = headers.get(</font><font face=
  "Courier New, Courier, monospace"><code><font face=
  "Courier New, Courier, monospace">key</font></code>);<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; <strong>if</strong>
  (valueList != <strong>null</strong>)<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
  <strong>return</strong> (T)parse(valueList); //parse as you
  wish...<br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; <strong>else</strong><br />
  &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp;
  <strong>return</strong> <strong>null</strong>;</font>
</p>
<p>
  <br />
</p>
<h1>
  <a name="ambiente" id="ambiente"></a>Environment<br />
</h1>
<p>
  <span style="font-weight: bold;">IMPORTANT:</span> The project
  must be demonstrated in the labs, with servers running in
  <span style="font-weight: bold;">at least two
  computers/containers</span>, either using existing hardware or
  student's hardware.
</p>
<p>
  Y<span style="font-weight: bold;">our system will be tested using
  the test program that will be made available soon</span>, which
  is divided in steps that test the different functionalities of
  your program -- you should use the client to check the progress
  of your project as you add new functionalities to your
  work.<br />
</p>
<p>
  The grading of your project will take into consideration the
  tests passed by your system -- so , you should guarantee that
  your systems passes as many test as possible (<span style=
  "font-weight: bold; color: red;">projects will be accepted even
  if they do not pass all tests</span>).
</p>
<p>
  <br />
</p>
<h1>
  <a name="report" id="report"></a>Written Report:
</h1>
<p>
  A written report <strong>must</strong> be delivered by each group
  describing their work and implementation. The report should have
  <strong>at most 4 pages</strong> <em>(any code that is found
  relevant should be delivered as an appendix that goes beyond the
  4 page limit)</em>.
</p>
<p>
  The report <strong>must</strong> cover the following topics.
</p>
<ul>
  <li>General description of the work performed by the students,
  clearly identifying which aspects were completed and fully
  implemented.<br />
  </li>
  <li>Limitations of the delivered code.<br />
    Students should include as annex a table that specifies which
    tests their code passed. For the failed tests, students should
    indicate whether the test has failed because the tested
    functionality was not implemented or because it had a
    bug.<br />
  </li>
  <li>Clear explanation of the mechanisms (i.e, protocols) employed
  for:
    <ul>
      <li>Accessing a remote service -- at least the following
      information should be provided: the API call invoked in the
      Twitter service; the design of the cache, if implemented.
      </li>
      <li>Security -- at least the following information should be
      provided: the goal of each mechanism (HTTPS, secret) and the
      contents of each keystore and truststore.
      </li>
      <li>Fault tolerance -- at least the following information
      should be provided: the design of the replication protocol(s)
      implemented and its guarantees, including the pseudo-code.
      </li>
    </ul>
  </li>
  <li>Discussion of the non-tricial implementation decisions taken
  by the students, when applicable, discussing these decisions in
  light of possible alternatives.
  </li>
</ul>
<p>
  The report can also cover aspects related with difficulties felt
  by the students during the execution of the project or other
  aspects that the students consider relevant.
</p>
<h1>
  <a name="delivery" id="delivery"></a>Delivery Rules:<br />
</h1>
<p>
  The <span style="font-weight: bold;">code of the project</span>
  should be delivered in electronic format, by uploading a zip file
  that includes:<br />
</p>
<ul>
  <li>all source files (src directory in the project)
  </li>
  <li>the sd2017-t2.props file
  </li>
  <li>the pom.xml file
  </li>
</ul>
<p>
  Use this <a target="_blank" style="font-weight: bold;" href=
  "https://goo.gl/forms/fmIS6LGcx9taeFNx1">**** link ****</a> to
  deliver your work (NOTE: you must login with your @campus
  account).<br />
  To keep the size of the zip archive small, zip full eclipse
  project minus the <strong>target</strong> folder that maven
  generates with the compiled classes and downloaded
  dependencies.<br />
  <strong>IMPORTANT</strong>: The name of the zip archive should
  be: SD2017-T2-NUM1.zip or SD2017-T2-NUM1-NUM2.zip
</p>
<p>
  <br />
</p>
<p>
  <span style="font-weight: bold;">NOTE:</span> You may deliver the
  project as many times as needed.
</p>
<p>
  <br />
</p>
<p>
  The <span style="font-weight: bold;">report</span> should be
  delivered in paper (no cover) in the DI secretary -- please put
  your name, number and lab class (P1.. P8) in the front
  page.<br />
</p>
<p>
  <br />
</p>
<p>
  <br />
</p>
<p>
  <br />
</p>
