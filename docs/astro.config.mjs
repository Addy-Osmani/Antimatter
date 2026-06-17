import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://antimatter.saifmukhtar.dev',
  integrations: [
    starlight({
      title: 'Antimatter',
      description: 'Open-source bridge ecosystem connecting your mobile device to your local AI agents.',
      tagline: 'Monitor your AI agent from anywhere. Secured by zero-trust cryptography.',
      logo: {
        src: './src/assets/logo.png',
        replacesTitle: false,
      },
      favicon: '/favicon.ico',
      social: [
        { icon: 'github', label: 'GitHub', href: 'https://github.com/saifmukhtar/antimatter' },
        { icon: 'external', label: 'saifmukhtar.dev', href: 'https://saifmukhtar.dev' },
      ],
      editLink: {
        baseUrl: 'https://github.com/saifmukhtar/antimatter/edit/main/docs-site/',
      },
      customCss: [
        './src/styles/custom.css',
      ],
      head: [
        {
          tag: 'meta',
          attrs: { name: 'theme-color', content: '#3B82F6' },
        },
        {
          tag: 'meta',
          attrs: { property: 'og:image', content: 'https://antimatter.saifmukhtar.dev/og.png' },
        },
      ],
      components: {
        Head: './src/components/Head.astro',
      },
      expressiveCode: {
        themes: ['github-dark', 'github-light'],
        styleOverrides: {
          borderRadius: '10px',
          frames: {
            shadowColor: 'rgba(59, 130, 246, 0.15)',
          },
        },
      },
      sidebar: [
        {
          label: '🚀 Start Here',
          items: [
            { label: 'Home', link: '/' },
            { label: 'Getting Started', link: '/getting-started/' },
            { label: 'Testing Status 🚨', link: '/testing-status/' },
          ],
        },
        {
          label: '📱 Mobile Apps',
          items: [
            { label: 'Android App', link: '/android/' },
            { label: 'iOS App', link: '/ios/' },
          ],
        },
        {
          label: '⚙️ System',
          items: [
            { label: 'Architecture', link: '/architecture/' },
            { label: 'Adapters', link: '/adapters/' },
            { label: 'WebSocket Protocol', link: '/protocol/' },
          ],
        },
        {
          label: '🔐 Security',
          items: [
            { label: 'Security & Zero Trust', link: '/security/' },
          ],
        },
        {
          label: '🛠 Development',
          items: [
            { label: 'Contributing Guide', link: '/contributing/' },
            { label: 'Testing Guide', link: '/testing/' },
            { label: 'iOS KVM Build Guide', link: '/macos-kvm-bridge/' },
          ],
        },
        {
          label: '📋 Project',
          items: [
            { label: 'Changelog', link: '/changelog/' },
            { label: 'Roadmap', link: '/roadmap/' },
            { label: 'Privacy Policy', link: '/privacy/' },
            { label: 'License', link: '/license/' },
            { label: 'About the Author', link: '/about/' },
          ],
        },
      ],
      lastUpdated: true,
      pagination: true,
    }),
  ],
});
