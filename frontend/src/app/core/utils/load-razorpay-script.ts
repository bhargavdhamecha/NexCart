// Lazily loads Razorpay's Checkout.js — scoped to whenever the payment page actually needs it,
// rather than every page paying for it via a site-wide <script> in index.html. Memoized so a
// second visit (or a second "Pay" click) doesn't re-fetch it.
let loadPromise: Promise<void> | null = null;

export function loadRazorpayCheckout(): Promise<void> {
  if ((window as any).Razorpay) {
    return Promise.resolve();
  }
  if (loadPromise) {
    return loadPromise;
  }

  loadPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve();
    script.onerror = () => {
      loadPromise = null; // allow a retry on the next call rather than caching a permanent failure
      reject(new Error('Failed to load Razorpay Checkout'));
    };
    document.head.appendChild(script);
  });

  return loadPromise;
}
