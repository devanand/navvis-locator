import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api';

@Component({
  selector: 'app-strategy',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './strategy.html',
  styleUrl: './strategy.css',
})
export class StrategyComponent implements OnInit {
  // 1. Declare state using signals
  current = signal<'JAVA' | 'POSTGIS'>('JAVA');
  switching = signal<boolean>(false);

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getStrategy().subscribe({
      next: (res) => {
        // 2. Update signal value
        this.current.set(res.strategy);
      },
    });
  }

  toggle() {
    const next = this.current() === 'JAVA' ? 'POSTGIS' : 'JAVA';
    this.switching.set(true);

    this.api.setStrategy(next).subscribe({
      next: (res) => {
        this.current.set(res.strategy);
        this.switching.set(false);
      },
      error: () => {
        this.switching.set(false);
      },
    });
  }
}
