import { ConversationData } from './types';

export const SAMPLE_CONVERSATIONS: Record<string, ConversationData> = {
  'operating-systems': {
    id: 'os-exam-notes-2024',
    title: 'Operating Systems - Comprehensive Exam Revision Notes',
    sourceUrl: 'https://chatgpt.com/share/672fc10a-8d14-8005-9271-e945c873f1d2',
    createdAt: new Date().toISOString(),
    messages: [
      {
        id: 'msg-1',
        role: 'user',
        authorName: 'You',
        content:
          'Can you create high-yield revision notes for my Operating Systems semester exam? Cover CPU scheduling, process synchronization (mutex vs semaphores, deadlock conditions), and virtual memory paging with formulas.',
      },
      {
        id: 'msg-2',
        role: 'assistant',
        authorName: 'ChatGPT',
        content: `### 1. CPU Scheduling Algorithms

CPU Scheduling deals with selecting a process from the ready queue and allocating the CPU to it.

#### Key Performance Metrics
- **Turnaround Time (TAT)** = Completion Time - Arrival Time
- **Waiting Time (WT)** = Turnaround Time - Burst Time
- **Response Time** = Time from arrival until the first time the CPU responds
- **Throughput** = Number of processes completed per unit time

| Algorithm | Preemption | Pros | Cons / Gotchas |
| :--- | :--- | :--- | :--- |
| **FCFS** (First Come First Serve) | Non-preemptive | Simple, FIFO queue | **Convoy Effect**: Short processes wait behind long ones |
| **SJF** (Shortest Job First) | Non-preemptive | Mathematically optimal average WT | Impossible to know future burst time in practice |
| **SRTF** (Shortest Remaining Time First)| Preemptive SJF | Lower wait time for interactive jobs | High context switch overhead |
| **Round Robin (RR)** | Preemptive (Time Quantum $q$) | Fair, responsive, no starvation | Sensitive to quantum $q$; too large $\\to$ FCFS, too small $\\to$ context switch thrashing |
| **Priority Scheduling** | Preemptive or Non-preemptive | Critical jobs run first | **Starvation**; solved using **Aging** |

---

### 2. Process Synchronization & Concurrency

#### The Critical Section Problem
A section of code accessing shared resources must satisfy three fundamental criteria:
1. **Mutual Exclusion**: If process $P_i$ is executing in its critical section, no other processes can be in theirs.
2. **Progress**: If no process is executing and some wish to enter, selection cannot be postponed indefinitely.
3. **Bounded Waiting**: There must be a limit on the number of times other processes enter before a waiting process is admitted.

#### Mutex vs. Counting Semaphore
\`\`\`c
// Classic Producer-Consumer with Semaphores
semaphore mutex = 1;      // Binary semaphore for mutual exclusion
semaphore empty = BUFFER_SIZE; // Empty slots in buffer
semaphore full = 0;       // Full slots in buffer

void producer() {
    while (1) {
        item = produce_item();
        wait(empty);      // Decrement empty slot count
        wait(mutex);      // Lock buffer
        insert_item(item);
        signal(mutex);    // Unlock buffer
        signal(full);     // Increment full slot count
    }
}
\`\`\`

#### Four Coffman Conditions for Deadlock
Deadlock can occur if and only if all four conditions hold simultaneously:
1. **Mutual Exclusion**: At least one resource must be held in a non-shareable mode.
2. **Hold and Wait**: A process holds resources while waiting to acquire others.
3. **No Preemption**: Resources cannot be preempted forcibly; released only voluntarily.
4. **Circular Wait**: A closed chain of processes exists, where each process holds resources needed by the next.

> **Deadlock Prevention vs Avoidance**: Prevention eliminates at least one Coffman condition statically. Avoidance uses **Banker's Algorithm** with runtime resource allocation graphs to ensure safe state trajectories.

---

### 3. Virtual Memory & Paging Formulas

- **Page Table**: Translates logical address (page number $p$, offset $d$) to physical frame $f$.
- **Effective Access Time (EAT)** with TLB (Translation Lookaside Buffer):
$$\\text{EAT} = h \\times (t_{\\text{TLB}} + t_{\\text{mem}}) + (1 - h) \\times (t_{\\text{TLB}} + 2 \\times t_{\\text{mem}})$$
Where $h$ is TLB hit ratio, $t_{\\text{TLB}}$ is TLB lookup time, and $t_{\\text{mem}}$ is physical memory access latency.

- **Page Fault Handling Steps**:
  1. Trap to OS on invalid page bit.
  2. Save process state and registers.
  3. Locate frame on backing store (disk/swap).
  4. Bring missing page into physical RAM frame.
  5. Update page table bit to valid.
  6. Restart the interrupted instruction.`,
      },
      {
        id: 'msg-3',
        role: 'user',
        authorName: 'You',
        content:
          'What is Belady\'s Anomaly and which page replacement algorithms suffer from it?',
      },
      {
        id: 'msg-4',
        role: 'assistant',
        authorName: 'ChatGPT',
        content: `### Belady's Anomaly

**Belady's Anomaly** is the counter-intuitive phenomenon where increasing the number of physical page frames results in an **increase** (rather than decrease) in the total number of page faults for certain memory reference strings.

#### Which Algorithms Suffer From It?
- **FIFO (First-In, First-Out)**: Suffixes from Belady's Anomaly because it is not a *Stack Algorithm*.
- **Second-Chance / Clock Algorithm**: Can also exhibit anomalous page fault spikes.

#### Which Algorithms Are Immune?
- **Optimal (OPT / MIN)**: Replaces the page that will not be used for the longest period in the future.
- **LRU (Least Recently Used)**: Replaces the page that has not been used for the longest period in the past.
- **LFU (Least Frequently Used)**: Based on reference counts.

> **Theorem (Stack Algorithms)**: Any page replacement algorithm where the set of pages in an $n$-frame memory is always a strict subset of pages in an $(n+1)$-frame memory satisfies the stack inclusion property and **cannot** exhibit Belady's Anomaly.`,
      },
    ],
    stats: {
      userMessages: 2,
      assistantMessages: 2,
      totalMessages: 4,
      estimatedPages: 3,
    },
  },
  'machine-learning': {
    id: 'ml-exam-notes-2024',
    title: 'Machine Learning - Core Foundations & Optimization',
    sourceUrl: 'https://chatgpt.com/share/672fc21b-9e20-8005-8472-a123f890b3c4',
    createdAt: new Date().toISOString(),
    messages: [
      {
        id: 'msg-ml-1',
        role: 'user',
        authorName: 'You',
        content:
          'Please give me concise study notes on Gradient Descent variants, Bias-Variance Tradeoff, and Cross-Entropy Loss with equations.',
      },
      {
        id: 'msg-ml-2',
        role: 'assistant',
        authorName: 'ChatGPT',
        content: `### 1. Bias-Variance Tradeoff

Total expected test error can be decomposed into three distinct components:

$$\\text{Total Error} = \\text{Bias}^2 + \\text{Variance} + \\sigma^2 \\text{ (Irreducible Error)}$$

- **High Bias (Underfitting)**: Model makes overly simplistic assumptions (e.g. fitting linear model to quadratic data). Both training error and test error are high.
- **High Variance (Overfitting)**: Model memorizes noise and specific peculiarities of the training set. Training error is very low, but validation/test error is significantly higher.

---

### 2. Loss Functions: Binary & Categorical Cross-Entropy

#### Binary Cross-Entropy (Log Loss)
For ground truth $y \\in \\{0, 1\\}$ and predicted probability $\\hat{y} \\in (0, 1)$:

$$\\mathcal{L}(y, \\hat{y}) = -\\left[ y \\log(\\hat{y}) + (1 - y) \\log(1 - \\hat{y}) \\right]$$

#### Multi-Class Categorical Cross-Entropy
For $K$ classes with one-hot encoded vector $y$ and Softmax probabilities $p$:

$$\\mathcal{L}(y, p) = -\\sum_{k=1}^K y_k \\log(p_k)$$

---

### 3. Gradient Descent Variants

\`\`\`python
# Adam Optimizer Core Logic (Kingma & Ba)
# m_t = beta1 * m_{t-1} + (1 - beta1) * g_t      (1st moment: running average of gradients)
# v_t = beta2 * v_{t-1} + (1 - beta2) * g_t^2    (2nd moment: uncentered variance)
# m_hat = m_t / (1 - beta1^t)                    (bias correction)
# v_hat = v_t / (1 - beta2^t)                    (bias correction)
# theta_t = theta_{t-1} - alpha * m_hat / (sqrt(v_hat) + eps)
\`\`\`

- **Batch GD**: Computes gradient across entire dataset. Stable, but slow and memory-intensive for large datasets.
- **SGD (Stochastic GD)**: Updates parameters per sample. Fast and escapes shallow local minima, but noisy convergence path.
- **Mini-batch GD**: Compromise (batch size $32 - 512$), leverages GPU vectorized tensor instructions.`,
      },
    ],
    stats: {
      userMessages: 1,
      assistantMessages: 1,
      totalMessages: 2,
      estimatedPages: 2,
    },
  },
};
