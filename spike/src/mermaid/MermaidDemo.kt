package io.ukata.athena.spike.mermaid

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private val DEMOS = listOf(
    "flowchart TD\n A[Start] --> B{Valid?}\n B -->|yes| C(Process)\n B -->|no| D[Reject]\n C --> E((Done))\n D --> E\n C -.-> F([Log])\n E ==> A\n A --> E",
    "graph LR\n A[Parse] --> B[Layer] --> C[Order] --> D[Draw]\n D -- retry --> A",
    "sequenceDiagram\n participant U as User\n participant R as Reader\n participant AI\n U->>R: select text\n R->>AI: explain(passage)\n AI-->>R: streamed answer\n R->>R: render\n R-->>U: popover",
    "classDiagram\n A <|-- B",
)

/** Run: `./kotlin run -m spike -- mermaid [file.mmd]` */
fun mermaidDemo(file: String?) = application {
    Window(onCloseRequest = ::exitApplication, title = "spike: mermaid") {
        MaterialTheme {
            val srcs = file?.let { listOf(java.io.File(it).readText()) } ?: DEMOS
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                srcs.forEach { MermaidView(it) }
            }
        }
    }
}
