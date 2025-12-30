<?php
// Handle folder creation
//if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_POST['new_folder'])) {
//    $newFolder = basename(trim($_POST['new_folder'])); // sanitize input
//    if (!is_dir($newFolder)) {
//        mkdir($newFolder);
//        $message = "✅ Folder '$newFolder' created successfully.";
//    } else {
//        $message = "⚠️ Folder '$newFolder' already exists.";
//    }
//}

// Get list of directories
$items = array_filter(glob('*'), function ($path) {
    return is_dir($path) || strtolower(pathinfo($path, PATHINFO_EXTENSION)) === 'pdf';
});

?>

<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>📁 Java HTTP Server Project Documentation</title>
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <link rel="stylesheet" href="/css/mermaid.css">
  <script type="module" src="/js/mermaid.js"></script>
  <style>
    body {
        font-family: Arial, sans-serif;
        background: #f7f7f7;
        color: #333;
        display: flex;
        flex-direction: column;
        align-items: center;
        margin: 0;
        padding: 40px;
    }
    h1 { margin-bottom: 20px; }
    form {
        margin-bottom: 20px;
        display: flex;
        gap: 10px;
    }
    input[type="text"] {
        padding: 8px 10px;
        border: 1px solid #ccc;
        border-radius: 5px;
    }
    button {
        background-color: #4CAF50;
        color: white;
        border: none;
        padding: 8px 14px;
        border-radius: 5px;
        cursor: pointer;
    }
    button:hover {
        background-color: #45a049;
    }
    .links ul {
        list-style-type: none;
        padding: 0;
        max-width: 400px;
        width: 100%;
    }
    .links li {
        list-style-type: none;
        background: #fff;
        margin: 5px 0;
        padding: 10px 14px;
        border-radius: 5px;
        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    a {
        text-decoration: none;
        color: #0077cc;
    }
    a:hover { text-decoration: underline; }
    .message {
        margin-bottom: 15px;
        color: #333;
        font-weight: bold;
    }
    @media (max-width: 900px){
        ul {
            font-size: 32px;
        }
    }
</style>
</head>
<body>
  <div class="description">
    <h1>📁 Java HTTP Server Project Documentation</h1>
    <h2>Overview</h2>
    <p>
      This is a comprehensive, self-hosted multi-tenant web hosting platform built from scratch in Java. 
      The system provides users with complete website hosting capabilities, including file management, 
      database access, PHP execution, blog creation, and media streaming—all through an intuitive 
      web-based interface.
    </p>
    <br>
    <h2>Architecture</h2>
    <h3>Backend (Java)</h3>
    <ul>
      <li><strong>Custom HTTP Server:</strong> Built on Java's com.sun.net.httpserver framework</li>
      <li><strong>Multi-threaded Execution:</strong> Separate thread pools for normal requests (20 threads) and video streaming (8 threads)</li>
      <li><strong>Dual Executor Design:</strong> DelegatingHandler intelligently routes video requests to a dedicated executor to prevent streaming from blocking standard file serving</li>
    </ul>
    <br>
    <h3>Frontend (JavaScript + PHP)</h3>
    <ul>
      <li><strong>Pure JavaScript:</strong> No frameworks—vanilla JS with modular ES6 imports</li>
      <li><strong>Ace Editor Integration:</strong> Full-featured code editor for in-browser file editing</li>
      <li><strong>PHP Interface Layer:</strong> Session management and authentication handled via PHP</li>
      <li><strong>Real-time Updates:</strong> Dynamic file browsing without page refreshes</li>
    </ul>
    <br>
    <h3>Database</h3>
    <ul>
      <li><strong>MySQL:</strong> User accounts, invitations, messages, and metadata</li>
      <li><strong>Per-User Isolation:</strong> Each user can have their own database with restricted access</li>
      <li><strong>Connection Pooling:</strong> Efficient database resource management</li>
    </ul>
    <br>
    <h2>Core Features</h2>
    <h3>1. Multi-Tenant User Management</h3>
    <ul>
      <li>User registration with invite codes</li>
      <li>Domain-based routing (each user gets their own domain)</li>
      <li>Per-user directory structure: /home/lukas/users/{username}/</li>
      <li>Session-based authentication with Java session management</li>
      <li>Password hashing via PHP's password_hash() function</li>
    </ul>
    <br>
    <h3>2. File Management System</h3>
    <p><strong>Web-based File Explorer:</strong> Navigate directories, create folders, upload/download files</p>
    <p><strong>In-browser Code Editor:</strong> Edit HTML, CSS, JavaScript, and PHP files with syntax highlighting</p>
    <p><strong>File Type Support:</strong></p>
    <ul>
      <li>Text files (HTML, CSS, JS, PHP)</li>
      <li>Images (JPEG, PNG, GIF, SVG, WebP, HEIC, TIFF, etc.)</li>
      <li>Videos (MP4, MOV, AVI) with streaming support</li>
      <li>PDFs</li>
    </ul>
    <p><strong>File Operations:</strong></p>
    <ul>
      <li>Drag-and-Drop Upload: Support for both individual files and entire folder structures</li>
      <li>Create, read, update, delete, rename, move files and folders</li>
      <li>Real-time file browser with visual feedback</li>
    </ul>
    <br>
    <h3>3. PHP Script Execution</h3>
    <ul>
      <li><strong>CGI Integration:</strong> Processes PHP files via php-cgi with custom configuration</li>
      <li><strong>Per-User PHP.ini:</strong> Each user has isolated PHP settings with security restrictions</li>
      <li><strong>Security Restrictions:</strong>
        <ul>
          <li>open_basedir restrictions to user's directory</li>
          <li>Disabled dangerous functions (exec, shell_exec, system, etc.)</li>
          <li>Resource limits (32MB memory, 5s execution time, 2MB upload size)</li>
        </ul>
      </li>
      <li><strong>GET/POST Support:</strong> Full request parameter handling</li>
      <li><strong>Cookie Management:</strong> PHP sessions and cookies properly forwarded</li>
      <li><strong>Header Forwarding:</strong> HTTP redirects and custom headers work correctly</li>
    </ul>
    <br>
    <h3>4. Blog Creation System</h3>
    <ul>
      <li><strong>Dual-File Storage:</strong>
        <ul>
          <li>HTML content stored in /blog/ directory</li>
          <li>Metadata .blog files stored in /static/</li>
        </ul>
      </li>
      <li><strong>Blog Editor:</strong> Rich text editing with metadata tracking</li>
      <li><strong>Blog Management:</strong> Create, edit, rename, delete blog posts</li>
      <li><strong>Direct Publishing:</strong> Blogs accessible via .blog extension</li>
    </ul>
    <br>
    <h3>5. Video Streaming</h3>
    <ul>
      <li><strong>HTTP Range Requests:</strong> Proper support for seekable video playback</li>
      <li><strong>Connection Limiting:</strong> Maximum 8 concurrent video streams to prevent server overload</li>
      <li><strong>Service Unavailable Handling:</strong> Returns 503 when stream limit reached</li>
      <li><strong>Efficient Streaming:</strong> Uses FileChannel.transferTo() for zero-copy streaming</li>
      <li><strong>Partial Content Support:</strong> Implements HTTP 206 responses for range requests</li>
    </ul>
    <br>
    <h3>6. Database Management</h3>
    <ul>
      <li><strong>Web-based Interface:</strong> Create tables, execute queries, manage data</li>
      <li><strong>Per-User Databases:</strong> Optional isolated database per user account</li>
      <li><strong>Query Builder:</strong> JSON-based query construction from frontend</li>
      <li><strong>Result Formatting:</strong> Automatic JSON serialization of query results</li>
      <li><strong>CRUD Operations:</strong> Full create, read, update, delete support</li>
    </ul>
    <br>
    <h3>7. Security Features</h3>
    <ul>
      <li><strong>Session Management:</strong> Java-based session system with cookie validation</li>
      <li><strong>Path Traversal Protection:</strong> All file paths normalized and validated</li>
      <li><strong>User Isolation:</strong> Users cannot access files outside their directory</li>
      <li><strong>File Type Validation:</strong> Upload restrictions based on allowed extensions</li>
      <li><strong>SQL Injection Protection:</strong> Prepared statements for all database queries</li>
      <li><strong>Admin Separation:</strong> Special handling for admin account with elevated permissions</li>
      <li><strong>Input Sanitization:</strong> File names and paths sanitized to prevent malicious input</li>
    </ul>
    <br>
    <h2>Technical Highlights</h2>
    <h3>Multipart Form Data Parser</h3>
    <ul>
      <li><strong>Custom Implementation:</strong> No external libraries—parses multipart/form-data manually</li>
      <li><strong>Binary-Safe:</strong> Handles file uploads with any encoding</li>
      <li><strong>Folder Structure Preservation:</strong> Maintains relative paths for folder uploads</li>
      <li><strong>Extension Validation:</strong> Whitelist-based file type checking</li>
    </ul>
    <br>
    <h3>Virtual Host System</h3>
    <ul>
      <li><strong>Domain Mapping:</strong> DomainsConfig.domainMap routes domains to user directories</li>
      <li><strong>Dynamic Resolution:</strong> Requests automatically routed based on Host header</li>
      <li><strong>Fallback Handling:</strong> Unknown domains serve default 404 page</li>
    </ul>
    <br>
    <h3>Content-Type Detection</h3>
    <ul>
      <li><strong>Extension-Based:</strong> Automatic MIME type detection for common file types</li>
      <li><strong>Custom Handlers:</strong> Special handling for Unity WebGL builds (.unityweb, Brotli compression)</li>
      <li><strong>Charset Support:</strong> UTF-8 encoding for text-based content</li>
    </ul>
    <br>
    <h3>Advanced Path Resolution</h3>
    <p>The RootHandler implements sophisticated fallback logic:</p>
    <ol>
      <li>Try exact file match</li>
      <li>Fall back to index.html</li>
      <li>Fall back to index.php</li>
      <li>Fall back to index.blog</li>
      <li>Serve 404 if nothing found</li>
    </ol>
    <br>
    <h2>File Structure</h2>
    <pre>/home/lukas/
├── users/
│   └── {username}/
│       ├── static/          # User's web-accessible files
│       │   ├── html/
│       │   ├── css/
│       │   ├── js/
│       │   ├── img/
│       │   └── php/
│       └── blog/            # Blog HTML files
├── JavaServerProject/
│   └── www/
│       └── static/          # Admin/default files
└── php_ini/
    └── {username}.ini       # Per-user PHP config</pre>
    <br>
    <h2>API Endpoints</h2>
    <p>The server exposes 20+ HTTP endpoints:</p>
    <p><strong>File Management:</strong></p>
    <ul>
      <li>/listAllFiles - List all files in a directory</li>
      <li>/deleteFile - Delete a specific file</li>
      <li>/getFileContent - Retrieve file contents</li>
      <li>/saveFile - Save or update file contents</li>
      <li>/upload - Upload single or multiple files</li>
      <li>/createFolder - Create a new directory</li>
      <li>/deleteFolder - Recursively delete a directory</li>
      <li>/moveIt - Move or rename files and folders</li>
    </ul>
    <p><strong>Blog Management:</strong></p>
    <ul>
      <li>/getBlogContent - Retrieve blog post HTML</li>
      <li>/saveBlog - Create or update blog posts</li>
      <li>/deleteBlog - Delete blog post and metadata</li>
      <li>/renameBlog - Rename blog post files</li>
    </ul>
    <p><strong>Authentication:</strong></p>
    <ul>
      <li>/create-session - Create new user session</li>
      <li>/check-session - Validate existing session</li>
      <li>/newuser - Register new user account</li>
      <li>/invite - Validate invitation codes</li>
    </ul>
    <p><strong>Database:</strong></p>
    <ul>
      <li>/query - Execute SQL queries</li>
      <li>/deleteRows - Delete database rows</li>
      <li>/createTable - Create new database tables</li>
    </ul>
    <p><strong>Utilities:</strong></p>
    <ul>
      <li>/game - Serve Unity WebGL game assets</li>
      <li>/questionForm - Handle contact form submissions</li>
    </ul>
    <br>
    <h2>User Interface</h2>
    <p>The web interface (index.php) provides:</p>
    <ul>
      <li><strong>Burger Menu Navigation:</strong> User info, domain settings, file manager, blog creator, logout</li>
      <li><strong>File Browser:</strong> Visual representation of directory structure with icons</li>
      <li><strong>Options Panel:</strong> Context-sensitive buttons for file operations</li>
      <li><strong>Code Editor:</strong> Full-screen Ace editor with syntax highlighting and auto-completion</li>
      <li><strong>Upload Zone:</strong> Drag-and-drop area with progress indicator</li>
      <li><strong>Mini Explorer:</strong> Modal file picker for "Save As" operations</li>
      <li><strong>Path Breadcrumbs:</strong> Current directory display with domain prefix</li>
    </ul>
    <br>
    <h2>Performance Optimizations</h2>
    <ol>
      <li><strong>Thread Pool Sizing:</strong> Separate pools prevent blocking</li>
      <li><strong>Stream Limiting:</strong> Prevents video streams from exhausting resources</li>
      <li><strong>File Channel Streaming:</strong> Zero-copy file transfers for large files</li>
      <li><strong>Lazy Loading:</strong> Files loaded on-demand, not preloaded</li>
      <li><strong>Connection Limits:</strong> Configurable maximum concurrent streams (8 for video)</li>
      <li><strong>Atomic Counter:</strong> Thread-safe stream tracking with AtomicInteger</li>
    </ol>
    <br>
    <h2>Deployment</h2>
    <ul>
      <li><strong>Port:</strong> HTTP on port 80 (requires root or port forwarding)</li>
      <li><strong>Single JAR:</strong> Compiles to standalone executable</li>
      <li><strong>Dependencies:</strong>
        <ul>
          <li>Java 11+ (uses com.sun.net.httpserver)</li>
          <li>PHP-CGI for script execution</li>
          <li>MySQL for database</li>
          <li>Standard Linux filesystem</li>
        </ul>
      </li>
      <li><strong>Configuration Files:</strong>
        <ul>
          <li>DomainsConfig.java - Domain to directory mapping</li>
          <li>PhpConfig.java - Domain to username mapping for PHP</li>
          <li>DBConfig.properties - Database connection settings</li>
        </ul>
      </li>
    </ul>
    <br>
    <h2>Use Cases</h2>
    <ol>
      <li><strong>Personal Web Hosting:</strong> Host your own websites and blogs</li>
      <li><strong>Development Environment:</strong> Test and deploy web projects</li>
      <li><strong>Client Hosting:</strong> Provide hosting services to multiple clients</li>
      <li><strong>Educational Platform:</strong> Learn web development with real hosting environment</li>
      <li><strong>Portfolio Hosting:</strong> Manage multiple portfolio sites under one server</li>
    </ol>
    <br>
    <h2>Key Classes and Components</h2>
    <h3>Handler Classes</h3>
    <ul>
      <li><strong>DelegatingHandler:</strong> Routes requests to appropriate executor based on content type</li>
      <li><strong>RootHandler:</strong> Main request handler with path resolution and content serving</li>
      <li><strong>UploadHandler:</strong> Processes single file uploads via multipart form data</li>
      <li><strong>UploadFolderHandler:</strong> Handles folder structure uploads</li>
      <li><strong>ListAllFiles:</strong> Returns categorized file listings as JSON</li>
      <li><strong>DeleteFileHandler:</strong> Deletes individual files</li>
      <li><strong>DeleteFolderHandler:</strong> Recursively removes directories</li>
      <li><strong>FetchFileContent:</strong> Retrieves file contents for editing</li>
      <li><strong>SaveFileHandler:</strong> Writes file contents to disk</li>
      <li><strong>CreateFolderHandler:</strong> Creates new directories</li>
      <li><strong>MoveItHandler:</strong> Moves/renames files and folders</li>
      <li><strong>SessionHandler:</strong> Creates user sessions</li>
      <li><strong>CheckJavaSession:</strong> Validates session tokens</li>
      <li><strong>SaveBlogHandler:</strong> Publishes blog posts</li>
      <li><strong>FetchBlogContentHandler:</strong> Retrieves blog content for editing</li>
      <li><strong>DeleteBlogHandler:</strong> Removes blog posts</li>
      <li><strong>RenameBlogHandler:</strong> Renames blog files</li>
    </ul>
    <br>
    <h3>Helper Methods</h3>
    <ul>
      <li><strong>handleMultipartFormData():</strong> Custom multipart parser for file uploads</li>
      <li><strong>runPhp():</strong> Executes PHP scripts via php-cgi</li>
      <li><strong>parseJsonToMap():</strong> Custom JSON parser without external libraries</li>
      <li><strong>deleteFolder():</strong> Recursive directory deletion</li>
      <li><strong>moveIt():</strong> File/folder move operations</li>
      <li><strong>phpHashPass():</strong> PHP password hashing integration</li>
      <li><strong>phpPassVerify():</strong> PHP password verification</li>
      <li><strong>sanitizeFileName():</strong> Input validation for file names</li>
      <li><strong>sanitizeUserName():</strong> Input validation for usernames</li>
    </ul>
    <br>
    <h2>Frontend Modules</h2>
    <h3>DocumentManager (docmanager.js)</h3>
    <p>Manages the user interface and file browser:</p>
    <ul>
      <li>File listing and navigation</li>
      <li>Editor initialization and control</li>
      <li>File operation UI (create, delete, rename)</li>
      <li>User dashboard and menu</li>
      <li>Modal dialogs and confirmations</li>
    </ul>
    <br>
    <h3>UploadManager (upmanager.js)</h3>
    <p>Handles file upload operations:</p>
    <ul>
      <li>Single and multiple file uploads</li>
      <li>Progress tracking with XMLHttpRequest</li>
      <li>Drag-and-drop support</li>
      <li>File validation and sanitization</li>
      <li>Blog content management</li>
    </ul>
    <br>
    <h2>Security Considerations</h2>
    <ul>
      <li><strong>No Directory Traversal:</strong> All paths normalized with Paths.normalize()</li>
      <li><strong>Whitelist Validation:</strong> Only approved file extensions allowed</li>
      <li><strong>Session Timeout:</strong> Sessions expire after inactivity</li>
      <li><strong>Password Security:</strong> PHP's password_hash() with bcrypt</li>
      <li><strong>SQL Prepared Statements:</strong> All queries use parameterized statements</li>
      <li><strong>PHP Sandboxing:</strong> open_basedir and disabled functions restrict PHP access</li>
      <li><strong>Input Sanitization:</strong> File names sanitized to alphanumeric + underscore</li>
    </ul>
    <br>
    <h2>Limitations and Known Issues</h2>
    <ul>
      <li><strong>Memory-Based Uploads:</strong> Entire uploads loaded into memory (not ideal for very large files)</li>
      <li><strong>No Streaming Uploads:</strong> Files must complete upload before processing</li>
      <li><strong>Limited Concurrent Streams:</strong> Maximum 8 video streams (configurable)</li>
      <li><strong>No Built-in Backup:</strong> Manual backup required</li>
      <li><strong>Single Server:</strong> No horizontal scaling or load balancing</li>
    </ul>
    <br>
    <h2>Future Enhancement Opportunities</h2>
    <ul>
      <li>SSL/TLS support (HTTPS)</li>
      <li>FTP/SFTP access</li>
      <li>Email hosting integration</li>
      <li>Automated backups with versioning</li>
      <li>Resource usage monitoring per user</li>
      <li>Traffic analytics and logging</li>
      <li>Custom domain management UI</li>
      <li>Node.js/Python runtime support</li>
      <li>WebSocket support for real-time features</li>
      <li>CDN integration for static assets</li>
      <li>Automated SSL certificate management (Let's Encrypt)</li>
      <li>Git integration for version control</li>
    </ul>
    <br>
    <h2>Summary</h2>
    <p>
      This is a production-ready, feature-complete web hosting platform built entirely from scratch 
      in Java. It demonstrates advanced Java networking, custom HTTP protocol handling, multipart 
      form parsing, PHP CGI integration, and comprehensive file management—all wrapped in a 
      user-friendly web interface built with vanilla JavaScript.
    </p>
    <p>
      The multi-tenant architecture with per-user isolation makes it suitable for hosting multiple 
      independent websites on a single server while maintaining security and resource boundaries. 
      The system handles everything from static file serving to dynamic PHP execution, database 
      management, blog publishing, and video streaming—providing a complete hosting solution 
      without relying on traditional web servers like Apache or Nginx for core functionality.
    </p>
    <p>
      With over 3000 lines of Java code and comprehensive JavaScript modules, this project 
      represents a deep understanding of web protocols, server architecture, security principles, 
      and full-stack development. It serves as both a functional hosting platform and a learning 
      resource for understanding how web servers work at a fundamental level.
    </p>
  </div>
  <?php if (!empty($message)): ?>
    <div class="message"><?= htmlspecialchars($message) ?></div>
    <?php endif; ?>

    <ul class="links">
    <?php foreach ($items as $item): ?>
        <li class="links"><a href="<?= urlencode($item) ?>"><?= str_replace("-", " ", htmlspecialchars($item)) ?></a></li>
    <?php endforeach; ?>
    </ul>
</body>
</html>